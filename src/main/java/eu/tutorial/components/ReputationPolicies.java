package eu.tutorial.components;

import java.util.HashMap;
import java.util.Locale;
import org.json.JSONObject;

public class ReputationPolicies {

    private final HashMap<String, String> policies; //Key is entityID, values are policy elements
    
    public ReputationPolicies(){
        policies=new HashMap<>();
    }

    
    public void addPolicy(String entityID, int id, double minReputation, double maxReputation, String actionDesc, int action, int actionRatio, double severityLevel) {
        
        if (!(policies.containsKey(entityID))) {
            StringBuilder policiesStr = new StringBuilder();
            
            if (policies != null && !(policies.isEmpty())) {
                policiesStr.append(",");
            }
            policiesStr.append(Integer.toString(id));
            policiesStr.append(":");
            policiesStr.append(String.format(Locale.US, "%.2f", minReputation));
            policiesStr.append(":");
            policiesStr.append(String.format(Locale.US, "%.2f", maxReputation));
            policiesStr.append(":");
            policiesStr.append(actionDesc);
            policiesStr.append(":");
            policiesStr.append(Integer.toString(action));
            policiesStr.append(":");
            policiesStr.append(Integer.toString(actionRatio));
            policiesStr.append(":");
            policiesStr.append(Double.toString(severityLevel));

            policies.put(entityID, policiesStr.toString());
            System.out.println("Policy Added: " + policiesStr.toString());
        }else{
            System.out.println("Policy exists for " + entityID);
        }
    }

    public void checkPoliciesToSend(ReputationSystemModel repModel, String entityID, JSONObject jsonToSend, double severityEvent) {
        
        String policiesforEntity = policies.get(entityID);

        if (policiesforEntity != null && (policiesforEntity.length() > 0)) {
            
            String[] policyArray = policiesforEntity.split(",");
            for (int i = 0; i < policyArray.length; i++) {
                String[] policyInfo = policyArray[i].split("[:]");
                
                double minReputation = Double.parseDouble(policyInfo[1]);
                double maxReputation = Double.parseDouble(policyInfo[2]);
                
                String actionDesc = policyInfo[3];
                int action = Integer.parseInt(policyInfo[4]);
                int actionRatio = Integer.parseInt(policyInfo[5]);
                double actualRep = repModel.getReputationScore(entityID);
                
                if ( actualRep > minReputation && actualRep < maxReputation) {
                    jsonToSend.put("actionDesc", actionDesc);
                    jsonToSend.put("action", action);
                    jsonToSend.put("actionRatio", actionRatio); 
                    //System.out.println("Policy found on min and MAX reputation interval" + jsonToSend);
                } 
                
                double eventSeverity = Double.parseDouble(policyInfo[6]);
                if (eventSeverity == severityEvent){
                    jsonToSend.put("actionDesc", actionDesc);
                    jsonToSend.put("action", action);
                    jsonToSend.put("actionRatio", actionRatio);
                    jsonToSend.put("eventySeverity", eventSeverity);
                    //System.out.println("Policy found on severityEvent" + jsonToSend);
                }
            }
        }else{
            System.out.println("Policy not found!");
        }
     
    }

}
