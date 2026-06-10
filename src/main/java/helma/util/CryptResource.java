/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Properties;
import java.util.StringTokenizer;

import helma.framework.repository.Resource;
import helma.framework.repository.Resource;

/**
 *  This file authenticates against a passwd source
 */
public class CryptResource {

    private Properties users;
    private CryptResource parentResource;
    private Resource resource;
    private long lastRead = 0;

    /**
     * Creates a new CryptSource object.
     *
     * @param resource ...
     * @param parentResource ...
     */
    public CryptResource(Resource resource, CryptResource parentResource) {
        this.resource = resource;
        this.parentResource = parentResource;
        users = new Properties();
    }

    /**
     *
     *
     * @param username ...
     * @param pw ...
     *
     * @return ...
     */
    public boolean authenticate(String username, String pw) {
        if (resource.exists() && (resource.lastModified() > lastRead)) {
            readFile();
        } else if (!resource.exists() && (users.size() > 0)) {
            users.clear();
        }

        String realpw = users.getProperty(username);

        if (realpw != null) {
            // Verifies modern PBKDF2 hashes as well as legacy Unix crypt and
            // MD5-hex entries, with a constant-time comparison.
            return PasswordHasher.verify(pw, realpw);
        } else {
            if (parentResource != null) {
                return parentResource.authenticate(username, pw);
            }
        }

        return false;
    }

    private synchronized void readFile() {
        BufferedReader reader = null;

        users = new Properties();

        try {
            reader = new BufferedReader(new StringReader(resource.getContent()));

            String line = reader.readLine();

            while (line != null) {
                StringTokenizer st = new StringTokenizer(line, ":");

                if (st.countTokens() > 1) {
                    users.put(st.nextToken(), st.nextToken());
                }

                line = reader.readLine();
            }
        } catch (Exception ignore) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception x) {
                }
            }

            lastRead = System.currentTimeMillis();
        }
    }

}

