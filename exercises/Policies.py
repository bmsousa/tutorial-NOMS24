import pika
import json
import random
import time
import sys

if len(sys.argv) != 2:
    print("Usage: python3 Policies.py deviceExample1")
    sys.exit(1)

try:
    entityID = sys.argv[1]
except ValueError:
    print("Please provide a valid options. deviceEXample1")
    entityID="deviceExample1"

credentials = pika.PlainCredentials('noms', 'tutorial')
parameters = pika.ConnectionParameters('localhost', 
                              5672, 'integration_environment',
                              credentials=credentials)

connection = pika.BlockingConnection(parameters)   

channel = connection.channel()

channel.queue_declare(queue='rs_queue', auto_delete=True)
# channel.queue_declare(queue='amon_test', durable=True)

exchangeName = 'reputationPolicies'
routingKeyName = 'rs_queue'

count = 0
count += 1

# Policies creation: 
# Action 1 - allow
# Action 2 - Deny
# Action 3 - Throtle communications
#

count = 1

dicio = {
    "id": count,
    "minReputationScore": 0.1,
    "maxReputationScore": 0.4,
    "actionDesc": "Deny",
    "action": 2,
    "actionRatio": 100,
    "entityID":entityID,
    "eventSeverity":3
}


channel.basic_publish(exchange=exchangeName,
                        routing_key=routingKeyName,
                        body=json.dumps(dicio),
                        properties=pika.BasicProperties(
                            delivery_mode=2,  # make message persistent
                        ))
print(" [%d] Sent %r" % (count, dicio))

