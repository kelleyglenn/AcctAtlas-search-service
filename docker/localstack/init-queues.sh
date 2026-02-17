#!/bin/bash
set -e
echo "Creating search-moderation-events queue..."
awslocal sqs create-queue --queue-name search-moderation-events
awslocal sqs create-queue --queue-name search-moderation-events-dlq
echo "Queues created:"
awslocal sqs list-queues
