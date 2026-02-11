#!/bin/bash
set -e
echo "Creating moderation-events queue..."
awslocal sqs create-queue --queue-name moderation-events
awslocal sqs create-queue --queue-name moderation-events-dlq
echo "Queues created:"
awslocal sqs list-queues
