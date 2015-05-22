#!/usr/bin/env bash

#node --v8-options to show more options
node --nouse-idle-notification --expose-gc --max-new-space-size=1024 --max-new-space-size=2048 --max-old-space-size=8192 ./webserver.js