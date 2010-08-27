/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.rs.cmdline;

import de.uniluebeck.itm.tr.rs.federator.FederatorRS;
import de.uniluebeck.itm.tr.rs.persistence.Comparison;
import de.uniluebeck.itm.tr.rs.persistence.EndpointPropertiesTestMap;
import de.uniluebeck.itm.tr.snaa.cmdline.server.Server;
import de.uniluebeck.itm.tr.snaa.federator.FederatorSNAA;
import eu.wisebed.testbed.api.rs.v1.*;
import eu.wisebed.testbed.api.snaa.v1.Action;
import eu.wisebed.testbed.api.snaa.v1.AuthenticationTriple;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static org.junit.Assert.*;


public class FederatorInmemoryTest {

    private Map<String, Set<String>> rsPrefixSet = null;

    private Map<String, Set<String>> snaaPrefixSet = null;

    private RS rsFederator = null;

    private FederatorSNAA snaaFederator = null;

    private Map<String, String> snaaEndpointPropertiesMapWisebed1 = EndpointPropertiesTestMap.SNAAPropertiesMapWisebed1;

    private Map<String, String> snaaEndpointPropertiesMapWisebed2 = EndpointPropertiesTestMap.SNAAPropertiesMapWisebed2;

    private Map<String, String> rsEndpointPropertiesMap = EndpointPropertiesTestMap.RSPropertiesMap;

    private List<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey> rsSecretAuthenticationKeyList =
            new LinkedList<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey>();

    private List<eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey> snaaSecretAuthenticationKeyList =
            new LinkedList<eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey>();

    private GregorianCalendar gregorianCalendarFrom = null;

    private GregorianCalendar gregorianCalendarTo = null;

    private Map<Integer, ConfidentialReservationData> reservationDataMap = new HashMap<Integer, ConfidentialReservationData>();

    private Map<Integer, List<SecretReservationKey>> reservationKeyMap = null;

    @Before
    public void setUp() throws Exception {
        //PropertiesMap SNAA1
        Properties SNAAProps1 = new Properties();
        for (String key : snaaEndpointPropertiesMapWisebed1.keySet()) {
            SNAAProps1.setProperty(key, snaaEndpointPropertiesMapWisebed1.get(key));
        }
        //PropertiesMap SNAA2
        Properties SNAAProps2 = new Properties();
        for (String key : snaaEndpointPropertiesMapWisebed2.keySet()) {
            SNAAProps2.setProperty(key, snaaEndpointPropertiesMapWisebed2.get(key));
        }

        //Properties RS
        Properties RSProps = new Properties();
        for (String key : rsEndpointPropertiesMap.keySet()) {
            RSProps.setProperty(key, rsEndpointPropertiesMap.get(key));
        }

        //starting SNAA-Server
        Server.startFromProperties(SNAAProps1);
        Server.startFromProperties(SNAAProps2);
        //starting RS-Server
        Main.startFromProperties(RSProps);

        List<String> urnPrefixe = new LinkedList<String>();
        urnPrefixe.add("urn:wisebed1:testbed1");
        urnPrefixe.add("urn:wisebed2:testbed1");

        Set<String> testbed1 = new HashSet<String>();
        testbed1.add(urnPrefixe.get(0));
        Set<String> testbed2 = new HashSet<String>();
        testbed2.add(urnPrefixe.get(1));

        snaaPrefixSet = new HashMap<String, Set<String>>();
        snaaPrefixSet.put("http://localhost:8080/snaa/dummy1", testbed1);
        snaaPrefixSet.put("http://localhost:8090/snaa/dummy1", testbed2);
        snaaFederator = new FederatorSNAA(snaaPrefixSet);

        rsPrefixSet = new HashMap<String, Set<String>>();
        rsPrefixSet.put("http://localhost:9090/rs/inmemory1", testbed1);
        rsPrefixSet.put("http://localhost:9090/rs/inmemory2", testbed2);
        rsFederator = new FederatorRS(rsPrefixSet);


        //creating SNAA-Authentication-Data
        LinkedList<AuthenticationTriple> snaaAuthenticationData = null;
        for (String prefix : urnPrefixe) {
            AuthenticationTriple snaaAuthenticationTriple = new AuthenticationTriple();
            snaaAuthenticationTriple.setUsername("Nils Rohwedder");
            snaaAuthenticationTriple.setPassword("");
            snaaAuthenticationTriple.setUrnPrefix(prefix);
            snaaAuthenticationData = new LinkedList<AuthenticationTriple>();
            snaaAuthenticationData.add(snaaAuthenticationTriple);
            snaaSecretAuthenticationKeyList.addAll(snaaFederator.authenticate(snaaAuthenticationData));
            Action action = new Action();
            action.setAction("reserve");
            assertTrue(snaaFederator.isAuthorized(snaaSecretAuthenticationKeyList, action));
        }

        //creating SecretAuthenticationKey
        for (eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey snaaKey : snaaSecretAuthenticationKeyList) {
            eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey rsKey =
                    new eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey();
            rsKey.setUsername(snaaKey.getUsername());
            rsKey.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
            rsKey.setUrnPrefix(snaaKey.getUrnPrefix());
            rsSecretAuthenticationKeyList.add(rsKey);
        }


        List<Data> dataList = new LinkedList<Data>();
        Data data = new Data();
        data.setUrnPrefix("urn:wisebed1:testbed1");
        data.setUrnPrefix("urn:wisebed2:testbed1");
        data.setUsername("Nils Rohwedder");
        dataList.add(data);

        List<String> urns = new LinkedList<String>();
        urns.add("urn:wisebed1:testbed1");
        urns.add("urn:wisebed2:testbed1");
        //Creating confi-data
        for (int i = 0; i < 5; i++) {
            Thread.sleep(100);
            ConfidentialReservationData confidentialReservationData = new ConfidentialReservationData();
            confidentialReservationData.getData().addAll(dataList);
            confidentialReservationData.getNodeURNs().addAll(urns);

            long millis = System.currentTimeMillis() + 200000;
            gregorianCalendarFrom = new GregorianCalendar();
            gregorianCalendarFrom.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            gregorianCalendarFrom.setTimeInMillis(millis);
            confidentialReservationData.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarFrom));

            gregorianCalendarTo = new GregorianCalendar();
            gregorianCalendarTo.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            gregorianCalendarTo.setTimeInMillis(millis + 50);
            confidentialReservationData.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarTo));

            reservationDataMap.put(i, confidentialReservationData);
        }
    }

    private boolean equals(ConfidentialReservationData reservationData,
                           ConfidentialReservationData reservationDataOther) {
        return Comparison.equals(reservationData, reservationDataOther);
    }

    @Test
    public void test()
            throws ReservervationConflictExceptionException, RSExceptionException, AuthorizationExceptionException,
            ReservervationNotFoundExceptionException, DatatypeConfigurationException {
        makeReservation();
        getReservationBeforeDeletion();
        getReservations();
        getConfindentialReservations();
        deleteReservationBeforeDeletion();

        //Error-testing
        getReservationAfterDeletion();
        deleteReservationAfterDeletion();
        //Exception-Testing
        makeReservationErrorTesting();
        getReservationErrorTesting();
        getReservationsErrorTesting();
    }

    private void getReservationsErrorTesting() {
        //testing on null
        try {
            rsFederator.getReservations(null, null);
            fail("Should have raised RSException");
        }
        catch (RSExceptionException e) {
        }
    }

    private void getReservationErrorTesting() throws ReservervationNotFoundExceptionException, RSExceptionException {
        //testing on null
        try {
            rsFederator.getReservation(null);
            fail("Should have raised RSException");
        }
        catch (RSExceptionException e) {
        }
        //testing on empty SecretReservationKey-List
        try {
            rsFederator.getReservation(Arrays.asList(new SecretReservationKey()));
            fail("Should have raised RSException");
        }
        catch (RSExceptionException e) {
        }
        //testing on wrong SecretReservationKey
        try {
            SecretReservationKey key = new SecretReservationKey();
            key.setSecretReservationKey("abcdefghijklmnopqrstuvwxyz");
            key.setUrnPrefix("urn:wisebed1:testbed1");
            rsFederator.getReservation(Arrays.asList(key));
            fail("Should have raised ReservationNotFoundException");
        }
        catch (ReservervationNotFoundExceptionException e) {
            // do nothing
        }
    }

    private void makeReservationErrorTesting()
            throws RSExceptionException, ReservervationConflictExceptionException, AuthorizationExceptionException {
        //testing on null
        try {
            rsFederator.makeReservation(null, null);
            fail("Should have raised an RSExceptionException");
        }
        catch (RSExceptionException e) {
            // do nothing
        }
        try {
            List<SecretAuthenticationKey> data = new LinkedList<SecretAuthenticationKey>();
            rsFederator.makeReservation(data, null);
            fail("Should have raised an RSExceptionException");
        }
        catch (RSExceptionException e) {
            // do nothing
        }
        //testing on not-served urn
        try {
            List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
            ConfidentialReservationData resData = new ConfidentialReservationData();
            resData.getNodeURNs().add("urn:not:served");
            rsFederator.makeReservation(authData, resData);
            fail("Should have raised an RSExceptionException");
        }
        catch (RSExceptionException e) {
            // do nothing
        }
        //testing on not valid authentication-data
        try {
            List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
            ConfidentialReservationData resData = new ConfidentialReservationData();
            resData.setFrom(createXMLGregorianCalendar(1 * 60 * 1000));
            resData.setTo(createXMLGregorianCalendar(5 * 60 * 1000));
            resData.getNodeURNs().add("urn:wisebed1:testbed1");
            rsFederator.makeReservation(authData, resData);
            fail("Should have raised an RSExceptionException");
        }
        catch (RSExceptionException e) {
            // do nothing
        }
        //testing if makeReservation on empty authenticationData and reservation-data returns empty SecretReservationKey-list
        List<SecretAuthenticationKey> authData = new LinkedList<SecretAuthenticationKey>();
        ConfidentialReservationData resData = new ConfidentialReservationData();
        resData.getNodeURNs();
        List<SecretReservationKey> list = rsFederator.makeReservation(authData, resData);
        assertSame(list.size(), 0);
    }

    private XMLGregorianCalendar createXMLGregorianCalendar(int msInFuture) {
        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            fail();
        }
        DateTime dateTime = new DateTime(System.currentTimeMillis());
        return datatypeFactory.newXMLGregorianCalendar(dateTime.plusMillis(msInFuture).toGregorianCalendar());
    }

    public void makeReservation()
            throws RSExceptionException, ReservervationConflictExceptionException, AuthorizationExceptionException {
        reservationKeyMap = new HashMap<Integer, List<SecretReservationKey>>();
        for (int i = 0; i < reservationDataMap.size(); i++) {

            List<SecretReservationKey> secretReservationKeyList = rsFederator.makeReservation(
                    rsSecretAuthenticationKeyList,
                    reservationDataMap.get(i)
            );
            reservationKeyMap.put(i, secretReservationKeyList);
        }
    }

    public void getReservationBeforeDeletion() throws RSExceptionException, ReservervationNotFoundExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            List<ConfidentialReservationData> reservationData = rsFederator.getReservation(tempKeyList);
            for (ConfidentialReservationData confidentialReservationData : reservationData) {
                for (Data data : confidentialReservationData.getData()) {
                    ConfidentialReservationData testData = reservationDataMap.get(i);

                    testData.getNodeURNs().clear();
                    testData.getNodeURNs().addAll(confidentialReservationData.getNodeURNs());

                    testData.getData().clear();
                    testData.getData().addAll(confidentialReservationData.getData());
                    /*for (Data userData : testData.getData()) {
                             assertEquals(userData.getSecretReservationKey(), this.secretReservationKey);
                         }*/

                    assertTrue(equals(testData, confidentialReservationData));
                }
            }
        }
    }

    public void getReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            try {
                rsFederator.getReservation(tempKeyList);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e) {
            }
        }
    }

    public void deleteReservationBeforeDeletion()
            throws RSExceptionException, ReservervationNotFoundExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            rsFederator.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
        }
    }

    public void deleteReservationAfterDeletion() throws RSExceptionException {
        for (int i = 0; i < reservationDataMap.size(); i++) {
            List<SecretReservationKey> tempKeyList = reservationKeyMap.get(i);
            try {
                rsFederator.deleteReservation(Collections.<SecretAuthenticationKey>emptyList(), tempKeyList);
                fail("Should have raised an ReservervationNotFoundExceptionException");
            }
            catch (ReservervationNotFoundExceptionException e) {
                ;
            }
        }
    }

    public void getReservations() throws RSExceptionException, DatatypeConfigurationException {
        for (Integer i : reservationDataMap.keySet()) {

            long from = reservationDataMap.get(i).getFrom().toGregorianCalendar().getTimeInMillis();
            long to = reservationDataMap.get(i).getTo().toGregorianCalendar().getTimeInMillis();

            //first interval : no overlap first direction
            XMLGregorianCalendar testFrom = createGregorianCalendar(from - 20);
            XMLGregorianCalendar testTo = createGregorianCalendar(to - 60);
            assertSame(rsFederator.getReservations(testFrom, testTo).size(), 0);

            //second interval : small overlap first direction
            testFrom = createGregorianCalendar(from - 20);
            testTo = createGregorianCalendar(to - 30);
            assertSame(rsFederator.getReservations(testFrom, testTo).size(), 2);

            //        //second interval : small overlap second direction
            testFrom = createGregorianCalendar(from + 30);
            testTo = createGregorianCalendar(to + 20);
            assertSame(rsFederator.getReservations(testFrom, testTo).size(), 2);

            //third interval : overlap on the same timeline first direction
            testFrom = createGregorianCalendar(from - 20);
            testTo = createGregorianCalendar(from);
            assertSame(rsFederator.getReservations(testFrom, testTo).size(), 0);

            //third interval : overlap on the same timeline second direction
            testFrom = createGregorianCalendar(to);
            testTo = createGregorianCalendar(to + 20);
            assertSame(rsFederator.getReservations(testFrom, testTo).size(), 0);

            //fourth interval : absolute overlap first direction
            testFrom = createGregorianCalendar(from + 5);
            testTo = createGregorianCalendar(to - 5);
            assertSame(rsFederator.getReservations(testFrom, testTo).size(), 2);

            //fourth interval : absolute overlap second direction
            testFrom = createGregorianCalendar(from - 5);
            testTo = createGregorianCalendar(to + 5);
            assertSame(rsFederator.getReservations(testFrom, testTo).size(), 2);
        }
    }

    private XMLGregorianCalendar createGregorianCalendar(long from) throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(from);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    public void getConfindentialReservations() throws RSExceptionException, DatatypeConfigurationException {
        for (Integer i : reservationDataMap.keySet()) {
            long from = reservationDataMap.get(i).getFrom().toGregorianCalendar().getTimeInMillis();
            long to = reservationDataMap.get(i).getTo().toGregorianCalendar().getTimeInMillis();

            GetReservations period = createPeriod(from, to);

            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 2);

            //first interval : no overlap first direction
            period = createPeriod(from - 20, to - 60);
            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 0);

            //second interval : small overlap first direction
            period = createPeriod(from - 20, to - 30);
            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 2);

            //        //second interval : small overlap second direction
            period = createPeriod(from + 30, to + 20);
            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 2);

            //third interval : overlap on the same timeline first direction
            period = createPeriod(from - 20, from);
            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 0);

            //third interval : overlap on the same timeline second direction
            period = createPeriod(to, to + 20);
            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 0);

            //fourth interval : absolute overlap first direction
            period = createPeriod(from + 5, to - 5);
            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 2);

            //fourth interval : absolute overlap second direction
            period = createPeriod(from - 5, to + 5);
            assertSame(rsFederator.getConfidentialReservations(rsSecretAuthenticationKeyList, period).size(), 2);

        }
    }

    private GetReservations createPeriod(long from, long to) throws DatatypeConfigurationException {
        GetReservations period = new GetReservations();
        GregorianCalendar gregFrom = new GregorianCalendar();
        GregorianCalendar gregTo = new GregorianCalendar();
        gregFrom.setTimeInMillis(from);
        gregTo.setTimeInMillis(to);
        period.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregFrom));
        period.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregTo));
        return period;
    }
}
