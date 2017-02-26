package net.openhft.chronicle.bytes;

import java.security.Provider;
import java.security.Security;
import java.util.Map;

/**
 * Created by peter on 07/02/2017.
 */
public class CipherPerfMain {
    public static void main(String[] args) {
        for (Provider providers : Security.getProviders()) {
            for (Map.Entry<Object, Object> entry : providers.entrySet()) {
                if (entry.getKey().toString().startsWith("Cipher."))
                    System.out.println(entry);
            }
        }
    }
}
