#!/bin/bash
## RESTARTING THE HUB
##
set -xo errexit pipefail
if [[ $CONTEXT == "jenkins" ]]; then
  RESTART_CMD="sudo salt-call state.sls \
  flightstats.hub.restart \
  saltenv=prod \
  --retcode-passthrough"
  else
  RESTART_CMD='LOCAL_RESTART_CMD'
fi
echo "Restarting active servers."

SERVERS=$(curl -qs http://hub.${CLUSTER}.flightstats.io/internal/deploy/text)
for server in ${SERVERS}; do
    ssh deploy@${server} \
    echo "server contents: $(ls -la)" \
    RESTART_CMD
done
