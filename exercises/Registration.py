import pika
import json
import random
import time
import sys

if len(sys.argv) != 2:
    print("Usage: python3 registration.py deviceExample1")
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

exchangeName = 'registration'
routingKeyName = 'rs_queue'

# Registration
dicio = {"entityID":entityID, 
         "type":"registration", 
         "info":"registraton of Entity", 
         "timestamp":str(time.time())
         }

print(dicio)

channel.basic_publish(exchange=exchangeName,
                        routing_key=routingKeyName,
                        body=json.dumps(dicio),
                        properties=pika.BasicProperties(
                            delivery_mode=2,  # make message persistent
                        ))
print("Sent %r" % ( dicio))

