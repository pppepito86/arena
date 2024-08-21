#!/bin/bash

is_number() {
  local str=$1
  [[ "$str" =~ ^[0-9]+$ ]] && return 0 || return 1
}

# CSV header
echo "problem_id, time_limit"

for problem_id in *; do
  if ! [[ "${problem_id}" =~ ^[0-9]+$ ]]; then
    # Skip in case the folder contains some other files.
    continue
  fi
   
  grade_properties_file="${problem_id}/problem/grade.properties"
  if ! [ -e "${grade_properties_file}" ]; then
    echo "grade.properties not found for problem ${problem_id}." >&2
    continue
  fi

  # We specifically need to check for rows starting with "time" because
  # there are also problems with have "io_time".
  time_limit=$(cat "${grade_properties_file}" | grep '^time' |  tr -d -c 0-9.)
  
  if ! [[ "${time_limit}" =~ ^[0-9\.]+$ ]]; then
    echo "Missing or misformatted time limit ${time_limit} for problem ${problem_id}." >&2
    continue
  fi
  
  echo "${problem_id}", "${time_limit}"
done

