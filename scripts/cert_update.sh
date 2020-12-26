#!/bin/bash
set -e

certbot renew
openssl pkcs12 -export -in /etc/letsencrypt/live/arena.infosbg.com/fullchain.pem
-inkey /etc/letsencrypt/live/arena.infosbg.com/privkey.pem -out
/root/.ssh/keystore.p12 -name arena -passout pass:<password>
/root/arena/bin/update.sh

