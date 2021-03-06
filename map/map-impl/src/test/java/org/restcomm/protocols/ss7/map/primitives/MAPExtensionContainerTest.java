/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.protocols.ss7.map.primitives;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.restcomm.protocols.ss7.map.MAPParameterFactoryImpl;
import org.restcomm.protocols.ss7.map.api.MAPParameterFactory;
import org.restcomm.protocols.ss7.map.api.primitives.ASNPCSExtentionImpl;
import org.restcomm.protocols.ss7.map.api.primitives.MAPExtensionContainerImpl;
import org.restcomm.protocols.ss7.map.api.primitives.MAPPrivateExtensionImpl;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.mobius.software.telco.protocols.ss7.asn.ASNDecodeResult;
import com.mobius.software.telco.protocols.ss7.asn.ASNParser;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNOctetString;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author sergey vetyutnev
 *
 */
public class MAPExtensionContainerTest {
    MAPParameterFactory mapServiceFactory = new MAPParameterFactoryImpl();

    public static MAPExtensionContainerImpl GetTestExtensionContainer() {
        MAPParameterFactory mapServiceFactory = new MAPParameterFactoryImpl();

        ArrayList<MAPPrivateExtensionImpl> al = new ArrayList<MAPPrivateExtensionImpl>();
        
        ASNOctetString octetString=new ASNOctetString();
        octetString.setValue(Unpooled.wrappedBuffer(new byte[] { 11, 12, 13, 14, 15 }));
        al.add(mapServiceFactory.createMAPPrivateExtension(Arrays.asList(1L, 2L, 3L, 4L), octetString));
        al.add(mapServiceFactory.createMAPPrivateExtension(Arrays.asList(1L, 2L, 3L, 6L), null));
        
        octetString=new ASNOctetString();
        octetString.setValue(Unpooled.wrappedBuffer(new byte[] { 21, 22, 23, 24, 25, 26 }));
        al.add(mapServiceFactory.createMAPPrivateExtension(Arrays.asList(1L, 2L, 3L, 5L), octetString));

        ASNPCSExtentionImpl pcs=new ASNPCSExtentionImpl();
        octetString=new ASNOctetString();
        octetString.setValue(Unpooled.wrappedBuffer(new byte[] { 31, 32, 33 }));
        pcs.setValue(octetString);
        MAPExtensionContainerImpl cnt = mapServiceFactory.createMAPExtensionContainer(al, pcs);

        return cnt;
    }

    public static Boolean CheckTestExtensionContainer(MAPExtensionContainerImpl extContainer) {
        if (extContainer == null || extContainer.getPrivateExtensionList().size() != 3)
            return false;

        for (int i = 0; i < 3; i++) {
            MAPPrivateExtensionImpl pe = extContainer.getPrivateExtensionList().get(i);
            Long[] lx = null;
            byte[] bx = null;

            switch (i) {
                case 0:
                    lx = new Long[] { 1L, 2L, 3L, 4L };
                    bx = new byte[] { 11, 12, 13, 14, 15 };
                    break;
                case 1:
                    lx = new Long[] { 1L, 2L, 3L, 6L };
                    bx = null;
                    break;
                case 2:
                    lx = new Long[] { 1L, 2L, 3L, 5L };
                    bx = new byte[] { 21, 22, 23, 24, 25, 26 };
                    break;
            }

            Long[] iddata=new Long[4];
            iddata=pe.getOId().toArray(iddata);
            if (pe.getOId() == null || !Arrays.equals(iddata, lx))
                return false;
            
            if (bx == null) {
                if (pe.getData() != null)
                    return false;
            } else {
            	ASNOctetString octetString=(ASNOctetString)pe.getData();
            	ByteBuf value=octetString.getValue();
            	byte[] data=new byte[value.readableBytes()];
            	value.readBytes(data);
            	
            	if (pe.getData() == null || !Arrays.equals(data, bx))
                    return false;
            }
        }

        byte[] by = new byte[] { 31, 32, 33 };
        ASNOctetString octetString=(ASNOctetString)extContainer.getPcsExtensions().getValue();
        ByteBuf value=octetString.getValue();
    	byte[] data=new byte[value.readableBytes()];
    	value.readBytes(data);
        if (extContainer.getPcsExtensions() == null || !Arrays.equals(data, by))
            return false;

        return true;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeTest
    public void setUp() {
    }

    @AfterTest
    public void tearDown() {
    }

    @Test(groups = { "functional.decode", "primitives" })
    public void testDecode() throws Exception {
    	ASNParser parser=new ASNParser(false);
    	parser.replaceClass(MAPExtensionContainerImpl.class);
    	
        byte[] data = this.getEncodedData();
        ASNDecodeResult result=parser.decode(Unpooled.wrappedBuffer(data));
        
        assertFalse(result.getHadErrors());
        assertTrue(result.getResult() instanceof MAPExtensionContainerImpl);
        MAPExtensionContainerImpl extCont = (MAPExtensionContainerImpl)result.getResult();
        assertEquals(CheckTestExtensionContainer(extCont), Boolean.TRUE);
    }

    @Test(groups = { "functional.encode", "primitives" })
    public void testEncode() throws Exception {
    	ASNParser parser=new ASNParser(false);
    	parser.replaceClass(MAPExtensionContainerImpl.class);
    	
    	byte[] data = this.getEncodedData();

        MAPExtensionContainerImpl extCont = (MAPExtensionContainerImpl) GetTestExtensionContainer();
        ByteBuf buffer=parser.encode(extCont);
        byte[] encodedData = new byte[buffer.readableBytes()];
        buffer.readBytes(encodedData);
        assertTrue(Arrays.equals(data, encodedData));
    }

    private byte[] getEncodedData() {
        return new byte[] { 48, 45, (byte) 160, 36, 48, 12, 6, 3, 42, 3, 4, 4, 5, 11, 12, 13, 14, 15, 48, 5, 6, 3, 42, 3, 6, 48, 13,
                6, 3, 42, 3, 5, 4, 6, 21, 22, 23, 24, 25, 26, (byte) 161, 5, 4, 3, 31, 32, 33 };
    }
}
