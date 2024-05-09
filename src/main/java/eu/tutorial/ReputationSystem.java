package eu.tutorial;

import org.json.JSONObject;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import eu.tutorial.components.ReputationPolicies;
import eu.tutorial.components.ReputationSystemModel;
import eu.tutorial.utils.RCUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ReputationSystem {

    private static final int RETRY_TIME_SECS = 2; // Seconds
    private static final String ACKNOWLEDGE_TYPE = "basic"; // basic or auto

    private Channel channel = null;
    private HashMap<String, String> params = null;
    private List<String> exchanges = null;

    private ReputationSystemModel reputationModel = null;
    private ReputationPolicies reputationPolicies = null;
    private boolean autoAck = false;
    private String consumerTag = null;
    
    public ReputationSystem(){
        try{
            reputationModel = new ReputationSystemModel();
            reputationPolicies = new ReputationPolicies();
            consumerTag = null;

            ConnectionFactory factory = new ConnectionFactory();
            this.params = new HashMap<>();
            //this.params = RCUtils.getEnvVariables();
            this.params.put("hosts", RCUtils.getEnvVariable("RMQ_HOST") );
            this.params.put("port", RCUtils.getEnvVariable("RMQ_PORT") );
            this.params.put("virtualHost", RCUtils.getEnvVariable("RMQ_VIRTUAL_HOST"));
            this.params.put("exchanges", RCUtils.getEnvVariable("RMQ_EXCHANGES"));
            this.params.put("queueName", RCUtils.getEnvVariable("RMQ_QUEUE_NAME"));
            this.params.put("routingKey", RCUtils.getEnvVariable("RMQ_ROUTING_KEY"));
            this.params.put("username", RCUtils.getEnvVariable("RMQ_USERNAME"));
            this.params.put("password", RCUtils.getEnvVariable("RMQ_PASSWORD"));
            this.params.put("ackType", ACKNOWLEDGE_TYPE);
            this.params.put("exchanges", RCUtils.getEnvVariable("RMQ_EXCHANGES"));

            // Exchange list
            this.exchanges = Arrays.asList(params.get("exchanges").split(",[ ]*"));

            factory.setHost(params.get("hosts"));
            factory.setPort(Integer.parseInt(params.get("port")));
            factory.setVirtualHost(params.get("virtualHost"));
            factory.setUsername(params.get("username"));
            factory.setPassword(params.get("password"));
            Connection connection = factory.newConnection();
            channel = connection.createChannel();

            if (channel.isOpen()) {
             
                channel.queueDeclare(params.get("queueName"), false, false, true, null);
            }

            // bind to queues
            for (Iterator<String> iter = this.exchanges.iterator(); iter.hasNext(); ){
                String ex = iter.next();

                //channel.queueBind(params.get("queueName"), ex, "rs_queue"); 
                channel.queueBind(params.get("queueName"), ex, "#");   
            }
            
            callBackReceive();
    
            }catch (IOException | TimeoutException e) {
                System.out.println("Reputation System error on create:" + e);
            }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        
        // Print defined variables:
        
        RCUtils.getEnvVariables();
        RCUtils.printEnvs();

        ReputationSystem repSys = new ReputationSystem();
        //
       
    }

    private void callBackReceive() throws IOException, TimeoutException {

        
        channel.basicQos(1);

        this.consumerTag = this.channel.basicConsume(this.params.get("queueName"), this.autoAck, "testConsumer",
                new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag,
                            Envelope envelope,
                            BasicProperties properties,
                            byte[] body)
                            throws IOException {

                        String exchangeName = envelope.getExchange();
                        String routingKey = envelope.getRoutingKey();

                        
                        long deliveryTag = envelope.getDeliveryTag();
                        // process the message components here ...
                        try {
                            System.out.println("RECEIVED MESSAGE from [" + exchangeName + " : " + routingKey + "]: "
                                    + new String(body, StandardCharsets.UTF_8));
                            
                            String receivedMessage = new String(body, StandardCharsets.UTF_8);
                            JSONObject jsonReceived = new JSONObject(receivedMessage);
                            ProcessExchanges(jsonReceived, exchangeName, routingKey, deliveryTag);
                            
                        } catch (Exception e) {
                            System.out.println("RMQReceiver error: " + e);
                        }
                        channel.basicAck(deliveryTag, false);
                    }
                });
        System.out.println("RabbitMQ consumer start consuming data");


    }

    public void ProcessExchanges(JSONObject jsonReceived, String exchangeName, String routingKey, long deliveryTag ){

        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put("exchange_name", exchangeName);

        JSONObject jsonToSend = new JSONObject();

        switch(exchangeName){
            case "registration": // Initialize an entity

                if (jsonReceived.has("entityID") && jsonReceived.has("type") && jsonReceived.has("info")) {
                    reputationModel.addNewDataProcessor(jsonReceived.getString("entityID"));
                    jsonToSend.put("currentScore", reputationModel.getReputationScore(jsonReceived.getString("entityID")));
                    jsonToSend.put("previousScore", 0.0);
                    jsonToSend.put("entityID", jsonReceived.getString("entityID"));
                }

                break;

            case "5gAUSF":
                if (jsonReceived.has("entityID") && jsonReceived.has("result") ) {
                    reputationModel.addNewDataProcessor(jsonReceived.getString("entityID"));
                    boolean anomaly = false;
                    String result=  jsonReceived.getString("result");
                    if (result.equalsIgnoreCase("success")) {
                        anomaly= false;
                    }else{
                        anomaly = true; // negative event
                    }
                    double severity=-1;
                    
                    //Get previous score before updating
                    double previousScore = reputationModel.getReputationScore(jsonReceived.getString("entityID"));

                    if (jsonReceived.has("severity")){
                        severity= jsonReceived.getDouble("severity");
                        reputationModel.updateReputationScoreSeverity(jsonReceived.getString("entityID"), anomaly, severity);
                    }else{ //update reputation
                        reputationModel.updateReputationScore(jsonReceived.getString("entityID"), anomaly);
                    }
                    
                    jsonToSend.put("currentScore", reputationModel.getReputationScore(jsonReceived.getString("entityID")));
                    jsonToSend.put("previousScore", previousScore);
                    jsonToSend.put("entityID", jsonReceived.getString("entityID"));
                    
                    //Check if there are policies:
                    reputationPolicies.checkPoliciesToSend(reputationModel, jsonReceived.getString("entityID"), jsonToSend, severity);
                }
                break;

            case "reputationPolicies":
                if (jsonReceived.has("entityID")  ) {
                    reputationPolicies.addPolicy(jsonReceived.getString("entityID"),
                        jsonReceived.getInt("id"),
                        jsonReceived.getDouble("minReputationScore"), jsonReceived.getDouble("maxReputationScore"),
                        jsonReceived.getString("actionDesc"), jsonReceived.getInt("action"),
                        jsonReceived.getInt("actionRatio"), jsonReceived.getDouble("eventSeverity"));
                }
                break;
        }
        
        // send json file to reputation_updates exchange
        if (!(jsonToSend.length() == 0) && jsonToSend.has("entityID") && jsonToSend.has("currentScore") && jsonToSend.has("previousScore")) {
            String sentMessage = "SENDING MESSAGE " + jsonToSend.toString() + " to reputationUpdates exchange";
            System.out.println(sentMessage);
            
            try {
                sendReputationUpdates("reputationUpdates", jsonToSend.toString());
            }catch(Exception e){
                System.out.println("Exception occured " + e);
            }
        }
    }

    public void sendReputationUpdates(String exchange, String message)
            throws IOException, InterruptedException {
        while (channel == null) {
            //System.out.println("Channel is being initialized. Retrying send message in " + RETRY_TIME_SECS + " seconds.");
            Thread.sleep(RETRY_TIME_SECS * 1000);
        }
        channel.basicPublish(exchange, "#", null, message.getBytes(StandardCharsets.UTF_8));
        System.out.println("Message sent to exchange '" + exchange + "' for recipient '"
                + new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8) + "'");
        
    }
}
