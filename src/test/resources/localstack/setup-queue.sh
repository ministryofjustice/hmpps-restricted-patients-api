#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2

aws --endpoint-url=http://localhost:4566 sns create-topic --name offender_events
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name rp_api_dlq
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name rp_api_queue
aws --endpoint-url=http://localhost:4566 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4566/queue/rp_api_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"XTERNAL_MOVEMENT_RECORD-INSERTED\"]}"}'
