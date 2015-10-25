#!/bin/bash
# This script is in the public domain, furnished "as is", without technical
# support, and with no warranty, express or implied, as to its usefulness
# for any purpose.

set -o errexit
set -o pipefail
export LC_ALL=C

einfo() { echo ">>> $*" >&2 ; }
ewarn() { echo "!!! $*" >&2 ; }
die() { (( $# > 0 )) && ewarn "${@}" ; exit 1 ; }

get_md5() { md5sum -b < "${1}" | cut -d' ' -f1 ; }

# A sed trick to extract the first "Changes since ..."
# block added in a staged ChangeLog file.
guess_changelog() {
  # the first ChangeLog (0.0.3) contains several relevant blocks
  if git status jsch/ChangeLog | { grep -q "new file:" && cat >/dev/null ; } ; then
    git diff --cached jsch/ChangeLog \
      | sed -n -e '/^+Changes/,${s|^+||;s|[[:space:]]*$||;p}'
  # other ChangeLog contain only one relevant blocks, and it always comes first
  elif git status jsch/ChangeLog | { grep -q "modified:" && cat >/dev/null ; } ; then
    git diff --cached jsch/ChangeLog \
      | sed -n -e '/^+.*[^[:space:]]/{/^+Changes/h;/^+Changes/!H;$!d;}' \
               -e 'x;/+Changes/{s|^+||;s|\n+|\n|g;s|[[:space:]]*\n|\n|g;p;q;}'
  fi
  # NOTE: the "&& cat >/dev/null" is to avoid SIGPIPE from "git" when "grep -q" exits
  # first, because this would be lethal in "piipefail" mode.
}

cd "$(dirname "$0")"
[[ -d downloads ]] || mkdir downloads
[[ -d tmp ]] || mkdir tmp

# Reset the git repo to the "init" tag (empty, but this script).
einfo "Resetting git repository..."
git reset init >/dev/null
# TODO: implement incremental mode? (add new releases on top of current history)

# A "jq" command to extract, from the sourceforge downloads RSS,
# a sorted TSV of relevant infos (URL, md5, pubDate).
jqcmd=".rss.channel.item \
| sort_by((.\"files:sf-file-id\".\"\$t\"|tonumber)) \
| .[] \
| [.\"media:content\".url, .\"media:content\".\"media:hash\".\"\$t\", .pubDate] \
| @tsv"

# Get the sourceforge downloads RSS, transform it to JSON, and then TSV.
# Keep that in a Bash array (one entry per TSV line).
SF_JSCH_RSS_URL="http://sourceforge.net/projects/jsch/rss?path=/jsch"
releases=$(curl -Ls "${SF_JSCH_RSS_URL}" | xml2json | jq -r "${jqcmd}") \
  || die "Failed to get the releases list"
readarray -t releases <<<"${releases}"

# For each release... (in chronological order)
for line in "${releases[@]}" ; do
  einfo "${line}"
  IFS=$'\t' read -r url md5 pubDate <<<"${line}"

  # guess version and file name from the URL
  file=${url%/download} ; file=${file##http*/}
  version=${file#jsch-} ; version=${version%.zip}
  file=downloads/${file}
  [[ ${version} =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] \
    || die "Something wrong with this line: ${line}"

  # keep existing zip file only if havnig the right md5
  if [[ -f ${file} ]] ; then
    file_md5=$(get_md5 "${file}")
    [[ ${md5} = ${file_md5} ]] || {
      ewarn "Deleting invalid existing file: ${file}"
      rm -f "${file}"
    }
  fi

  # download the zip file if missing
  if [[ -f ${file} ]] ; then
    einfo "Reusing existing file: ${file}"
  else
    einfo "Downloading ${file}"
    wget -nv -t2 -O "${file}" "${url}"
    file_md5=$(get_md5 "${file}")
    [[ ${md5} = ${file_md5} ]] || die "Invalid md5 for ${file}"
  fi

  # extract zip file and replace "jsch" directory
  rm -rf tmp/* && unzip -q -d tmp "${file}"
  rm -rf jsch && mv tmp/"jsch-${version}" jsch

  # stage "jsch" for the release commit
  git reset >/dev/null && git add jsch

  # compute the commit log message
  commit_msg="JSCH version ${version}"
  changelog=$(guess_changelog)
  if [[ -n ${changelog} ]] ; then
    commit_msg="${commit_msg}"$'\n'$'\n'"${changelog}"
  fi

  # commit and tag the release
  einfo "git commit for ${version}:"$'\n'"${commit_msg}"
  git commit \
    --author="JCraft <info@jcraft.com>" \
    --date="${pubDate}" \
    -m "${commit_msg}" \
    || die "git commit failed"
  git tag -f "jsch-${version}" || die "git tag failed"
done
