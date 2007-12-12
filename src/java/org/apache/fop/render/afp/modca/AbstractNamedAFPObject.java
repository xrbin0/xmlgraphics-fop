/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.render.afp.modca;
import java.io.UnsupportedEncodingException;

/**
 * This is the base class for all named data stream objects.
 * A named data stream object has an 8 byte EBCIDIC name.
 */
public abstract class AbstractNamedAFPObject extends AbstractAFPObject {
        
    /**
     * The actual name of the object
     */
    protected String name = null;
    
    /**
     * The name of the object in EBCIDIC bytes
     */
    protected byte[] nameBytes;

    /**
     * Default constructor
     */
    protected AbstractNamedAFPObject() {
    }

    private static final int DEFAULT_NAME_LENGTH = 8;

    /**
     * @return the name length of this object
     */
    protected int getNameLength() {
        return DEFAULT_NAME_LENGTH;
    }

    /**
     * Constructor for the ActiveEnvironmentGroup, this takes a
     * name parameter which should be 8 characters long.
     * @param name the object name
     */
    public AbstractNamedAFPObject(String name) {
        int nameLen = getNameLength();
        if (name.length() < nameLen) {
            this.name = (name + "       ").substring(0, nameLen);
        } else if (name.length() > nameLen) {
            log.warn("Constructor:: name truncated to " + nameLen + " chars: " + name);
            this.name = name.substring(0, nameLen);
        } else {
            this.name = name;            
        }
        
        try {
            
            this.nameBytes = name.getBytes(AFPConstants.EBCIDIC_ENCODING);
            
        } catch (UnsupportedEncodingException usee) {
            
            this.nameBytes = name.getBytes();
            log.warn(
                "Constructor:: UnsupportedEncodingException translating the name "
                + name);
            
        }
        
    }    
}
