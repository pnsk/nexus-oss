/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.wonderland.model;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authToken", propOrder = {
    "u",
    "p"
})
@XmlRootElement(name = "authToken")
@Generated(value = "XJC 2.2.11", date = "2015-02-01T14:56:11")
public class AuthTokenXO {

    @XmlElement(required = true)
    @JsonProperty("u")
    protected String u;
    @XmlElement(required = true)
    @JsonProperty("p")
    protected String p;

    /**
     * Gets the value of the u property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getU() {
        return u;
    }

    /**
     * Sets the value of the u property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setU(String value) {
        this.u = value;
    }

    /**
     * Gets the value of the p property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getP() {
        return p;
    }

    /**
     * Sets the value of the p property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setP(String value) {
        this.p = value;
    }

    public AuthTokenXO withU(String value) {
        setU(value);
        return this;
    }

    public AuthTokenXO withP(String value) {
        setP(value);
        return this;
    }

}
