package eu.tutorial.utils;
import java.util.*;

public class RCUtils {
    private static final Object envlock = new Object();
    private static Map<String,String> env;

    //Thread-safe, double check sync, broken for java under 1.5
    public static  Map<String,String> getEnvVariables(){
        if (env == null) {
            synchronized (envlock) {    // While we were waiting for the lock, another
                if (env == null) {      // thread may have instantiated the object.
                    env = System.getenv();
                }
            }
        }
        return env;
    }

    public static void printEnvs(){
        String envstr="";
        StringBuilder sb = new StringBuilder();
        for (String envName : env.keySet()) {
            envstr = sb.append(String.format("%s=%s%n",
                    envName,
                    env.get(envName))).toString();
        }
        if(env==null)
            System.out.println("There are no variables to printBytes.\n");
        else
            System.out.println(envstr);
    }

    @Nullable
    public static String getEnvVariable(String variable){
        return getEnvVariables().get(variable);
    }

}
