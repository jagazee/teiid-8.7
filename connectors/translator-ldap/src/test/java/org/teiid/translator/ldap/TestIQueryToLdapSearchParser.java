/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.translator.ldap;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.SortKey;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.teiid.cdk.CommandBuilder;
import org.teiid.core.types.DataTypeManager;
import org.teiid.language.Command;
import org.teiid.language.Select;
import org.teiid.language.Update;
import org.teiid.metadata.Column;
import org.teiid.metadata.Column.SearchType;
import org.teiid.metadata.MetadataStore;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.CompositeMetadataStore;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.unittest.RealMetadataFactory;
import org.teiid.translator.TranslatorException;


/** 
 * Test IQueryToLdapSearchParser.  
 */
/**
 * @author mdrilling
 *
 */
@SuppressWarnings({"nls"})
public class TestIQueryToLdapSearchParser {

	/**
     * Get Resolved Command using SQL String and metadata.
     */
    public Command getCommand(String sql, QueryMetadataInterface metadata) {
    	CommandBuilder builder = new CommandBuilder(metadata);
    	return builder.getCommand(sql);
    }
    
	/**
     * Helper method for testing the provided LDAPSearchDetails against expected values
     * @param searchDetails the LDAPSearchDetails object
     * @param expectedContextName the expected context name
     * @param expectedContextFilter the expected context filter string
     * @param expectedAttrNameList list of expected attribute names
     * @param expectedCountLimit the expected count limit
     * @param expectedSearchScope the expected search scope
     * @param expectedSortKeys the expected sortKeys list.
     */
    public void helpTestSearchDetails(final LDAPSearchDetails searchDetails, final String expectedContextName,
    		final String expectedContextFilter, final List<String> expectedAttrNameList, final long expectedCountLimit, 
    		final int expectedSearchScope, final SortKey[] expectedSortKeys) {
    	
    	// Get all of the actual values
        String contextName = searchDetails.getContextName();
        String contextFilter = searchDetails.getContextFilter();
        List<Column> attrList = searchDetails.getElementList();
        long countLimit = searchDetails.getCountLimit();
    	int searchScope = searchDetails.getSearchScope();
    	SortKey[] sortKeys = searchDetails.getSortKeys();
    	
    	// Compare actual with Expected
    	assertEquals(expectedContextName, contextName);
    	assertEquals(expectedContextFilter, contextFilter);
    	
    	assertEquals(attrList.size(),expectedAttrNameList.size());
    	Iterator<Column> iter = attrList.iterator();
    	Iterator<String> eIter = expectedAttrNameList.iterator();
    	while(iter.hasNext()&&eIter.hasNext()) {
			String actualName = IQueryToLdapSearchParser.getNameFromElement(iter.next());
			String expectedName = eIter.next();
			assertEquals(actualName, expectedName);
    	}

    	assertEquals(expectedCountLimit, countLimit);
    	assertEquals(expectedSearchScope, searchScope);
    	assertArrayEquals(expectedSortKeys, sortKeys);
    }

	/**
     * Test a Query without criteria
     */
    @Test public void testSelectFrom1() throws Exception {
        LDAPSearchDetails searchDetails = helpGetSearchDetails("SELECT UserID, Name FROM LdapModel.People"); //$NON-NLS-1$
        
        //-----------------------------------
        // Set Expected SearchDetails Values
        //-----------------------------------
        String expectedContextName = "ou=people,dc=metamatrix,dc=com"; //$NON-NLS-1$
        String expectedContextFilter = "(objectClass=*)"; //$NON-NLS-1$
        
        List<String> expectedAttrNameList = new ArrayList<String>();
        expectedAttrNameList.add("uid"); //$NON-NLS-1$
        expectedAttrNameList.add("cn"); //$NON-NLS-1$
        
        long expectedCountLimit = -1;
        int expectedSearchScope = SearchControls.ONELEVEL_SCOPE;
        SortKey[] expectedSortKeys = null;
        
        helpTestSearchDetails(searchDetails, expectedContextName, expectedContextFilter, expectedAttrNameList,
        		expectedCountLimit, expectedSearchScope, expectedSortKeys);
        
    }
    
    @Test public void testUpdateNull() throws Exception {
        String sql = "update LdapModel.People set userid = 1, name = null where dn = 'x'"; //$NON-NLS-1$

        QueryMetadataInterface metadata = exampleLdap();
        
        Update query = (Update)getCommand(sql, metadata);
        
        LDAPExecutionFactory config = new LDAPExecutionFactory();
    	
        LdapContext context = Mockito.mock(LdapContext.class);
        
        Mockito.stub(context.lookup("")).toReturn(context);
        
		LDAPUpdateExecution lue = new LDAPUpdateExecution(query, context);
        
        lue.execute();
        ArgumentCaptor<ModificationItem[]> captor = ArgumentCaptor.forClass(ModificationItem[].class);
        Mockito.verify(context).modifyAttributes(ArgumentCaptor.forClass(String.class).capture(), captor.capture());
        ModificationItem[] modifications = captor.getValue();
        assertEquals(2, modifications.length);
        assertEquals("uid: 1", modifications[0].getAttribute().toString());
        assertEquals("cn: null", modifications[1].getAttribute().toString());
    }
    
    @Test public void testUpdateArray() throws Exception {
        String sql = "update LdapModel.People set userid = 1, vals = ('a','b') where dn = 'x'"; //$NON-NLS-1$

        QueryMetadataInterface metadata = exampleLdap();
        
        Update query = (Update)getCommand(sql, metadata);
        
        LDAPExecutionFactory config = new LDAPExecutionFactory();
    	
        LdapContext context = Mockito.mock(LdapContext.class);
        
        Mockito.stub(context.lookup("")).toReturn(context);
        
		LDAPUpdateExecution lue = new LDAPUpdateExecution(query, context);
        
        lue.execute();
        ArgumentCaptor<ModificationItem[]> captor = ArgumentCaptor.forClass(ModificationItem[].class);
        Mockito.verify(context).modifyAttributes(ArgumentCaptor.forClass(String.class).capture(), captor.capture());
        ModificationItem[] modifications = captor.getValue();
        assertEquals(2, modifications.length);
        assertEquals("uid: 1", modifications[0].getAttribute().toString());
        assertEquals("vals: a, b", modifications[1].getAttribute().toString());
    }
    
	/**
     * Test a Query with a criteria
     */
    @Test public void testSelectFromWhere1() throws Exception {
    	LDAPSearchDetails searchDetails = helpGetSearchDetails("SELECT UserID, Name FROM LdapModel.People WHERE Name = 'R%'"); //$NON-NLS-1$
        
        //-----------------------------------
        // Set Expected SearchDetails Values
        //-----------------------------------
        String expectedContextName = "ou=people,dc=metamatrix,dc=com"; //$NON-NLS-1$
        String expectedContextFilter = "(cn=R%)"; //$NON-NLS-1$
        
        List<String> expectedAttrNameList = new ArrayList<String>();
        expectedAttrNameList.add("uid"); //$NON-NLS-1$
        expectedAttrNameList.add("cn"); //$NON-NLS-1$
        
        long expectedCountLimit = -1;
        int expectedSearchScope = SearchControls.ONELEVEL_SCOPE;
        SortKey[] expectedSortKeys = null;
        
        helpTestSearchDetails(searchDetails, expectedContextName, expectedContextFilter, expectedAttrNameList,
        		expectedCountLimit, expectedSearchScope, expectedSortKeys);
        
    }
    
	/**
     * Test a Query with a criteria
     */
    @Test public void testEscaping() throws Exception {
    	LDAPSearchDetails searchDetails = helpGetSearchDetails("SELECT UserID, Name FROM LdapModel.People WHERE Name = 'R*'"); //$NON-NLS-1$
        
        //-----------------------------------
        // Set Expected SearchDetails Values
        //-----------------------------------
        String expectedContextName = "ou=people,dc=metamatrix,dc=com"; //$NON-NLS-1$
        String expectedContextFilter = "(cn=R\\2a)"; //$NON-NLS-1$
        
        List<String> expectedAttrNameList = new ArrayList<String>();
        expectedAttrNameList.add("uid"); //$NON-NLS-1$
        expectedAttrNameList.add("cn"); //$NON-NLS-1$
        
        long expectedCountLimit = -1;
        int expectedSearchScope = SearchControls.ONELEVEL_SCOPE;
        SortKey[] expectedSortKeys = null;
        
        helpTestSearchDetails(searchDetails, expectedContextName, expectedContextFilter, expectedAttrNameList,
        		expectedCountLimit, expectedSearchScope, expectedSortKeys);
        
    }
    
    @Test public void testNot() throws Exception {
    	LDAPSearchDetails searchDetails = helpGetSearchDetails("SELECT UserID, Name FROM LdapModel.People WHERE not (Name like 'R%' or Name like 'S%')"); //$NON-NLS-1$
        
        //-----------------------------------
        // Set Expected SearchDetails Values
        //-----------------------------------
        String expectedContextName = "ou=people,dc=metamatrix,dc=com"; //$NON-NLS-1$
        String expectedContextFilter = "(&(!(cn=R*))(!(cn=S*)))"; //$NON-NLS-1$
        
        List<String> expectedAttrNameList = new ArrayList<String>();
        expectedAttrNameList.add("uid"); //$NON-NLS-1$
        expectedAttrNameList.add("cn"); //$NON-NLS-1$
        
        long expectedCountLimit = -1;
        int expectedSearchScope = SearchControls.ONELEVEL_SCOPE;
        SortKey[] expectedSortKeys = null;
        
        helpTestSearchDetails(searchDetails, expectedContextName, expectedContextFilter, expectedAttrNameList,
        		expectedCountLimit, expectedSearchScope, expectedSortKeys);
        
    }
    
    @Test public void testGT() throws Exception {
    	LDAPSearchDetails searchDetails = helpGetSearchDetails("SELECT UserID, Name FROM LdapModel.People WHERE Name > 'R'"); //$NON-NLS-1$
        
        //-----------------------------------
        // Set Expected SearchDetails Values
        //-----------------------------------
        String expectedContextName = "ou=people,dc=metamatrix,dc=com"; //$NON-NLS-1$
        String expectedContextFilter = "(!(cn<=R))"; //$NON-NLS-1$
        
        List<String> expectedAttrNameList = new ArrayList<String>();
        expectedAttrNameList.add("uid"); //$NON-NLS-1$
        expectedAttrNameList.add("cn"); //$NON-NLS-1$
        
        long expectedCountLimit = -1;
        int expectedSearchScope = SearchControls.ONELEVEL_SCOPE;
        SortKey[] expectedSortKeys = null;
        
        helpTestSearchDetails(searchDetails, expectedContextName, expectedContextFilter, expectedAttrNameList,
        		expectedCountLimit, expectedSearchScope, expectedSortKeys);
    }
    
    @Test public void testLT() throws Exception {
    	LDAPSearchDetails searchDetails = helpGetSearchDetails("SELECT UserID, Name FROM LdapModel.People WHERE Name < 'R'"); //$NON-NLS-1$
        
        //-----------------------------------
        // Set Expected SearchDetails Values
        //-----------------------------------
        String expectedContextName = "ou=people,dc=metamatrix,dc=com"; //$NON-NLS-1$
        String expectedContextFilter = "(!(cn>=R))"; //$NON-NLS-1$
        
        List<String> expectedAttrNameList = new ArrayList<String>();
        expectedAttrNameList.add("uid"); //$NON-NLS-1$
        expectedAttrNameList.add("cn"); //$NON-NLS-1$
        
        long expectedCountLimit = -1;
        int expectedSearchScope = SearchControls.ONELEVEL_SCOPE;
        SortKey[] expectedSortKeys = null;
        
        helpTestSearchDetails(searchDetails, expectedContextName, expectedContextFilter, expectedAttrNameList,
        		expectedCountLimit, expectedSearchScope, expectedSortKeys);
    }
    
	private LDAPSearchDetails helpGetSearchDetails(String queryString) throws TranslatorException {
    	QueryMetadataInterface metadata = exampleLdap();
    	
    	Select query = (Select)getCommand(queryString, metadata);
    	
    	LDAPExecutionFactory config = new LDAPExecutionFactory();
    	
    	IQueryToLdapSearchParser searchParser = new IQueryToLdapSearchParser(config);

        LDAPSearchDetails searchDetails = searchParser.translateSQLQueryToLDAPSearch(query);
		return searchDetails;
	}
    
    public static QueryMetadataInterface exampleLdap() {
    	MetadataStore metadataStore = new MetadataStore();

        // Create models
        Schema ldapModel = RealMetadataFactory.createPhysicalModel("LdapModel", metadataStore); //$NON-NLS-1$
        
        // Create physical groups
        Table table = RealMetadataFactory.createPhysicalGroup("People", ldapModel); //$NON-NLS-1$
        table.setNameInSource("ou=people,dc=metamatrix,dc=com"); //$NON-NLS-1$
                
        // Create physical elements
        String[] elemNames = new String[] {
            "UserID", "Name", "dn", "vals"  //$NON-NLS-1$ //$NON-NLS-2$
        };
        String[] elemTypes = new String[] {  
            DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.STRING, "string[]"
        };
        
        List<Column> cols = RealMetadataFactory.createElements(table, elemNames, elemTypes);
        
        // Set name in source on each column
        String[] nameInSource = new String[] {
           "uid", "cn", "dn"             //$NON-NLS-1$ //$NON-NLS-2$  
        };
        for(int i=0; i<2; i++) {
            Column obj = cols.get(i);
            obj.setNameInSource(nameInSource[i]);
        }
        
        // Set column-specific properties
        for(int i=1; i<2; i++) {
            cols.get(i).setSearchType(SearchType.Unsearchable);
        }
        
        // Create the facade from the store
        return new TransformationMetadata(null, new CompositeMetadataStore(metadataStore), null, RealMetadataFactory.SFM.getSystemFunctions(), null);
    }    
    
    @Test public void testLike() throws Exception {
    	String query = "Name like 'R*%'";
    	String expectedContextFilter = "(cn=R\\2a*)"; //$NON-NLS-1$
    	
    	helpTestLike(query, expectedContextFilter);
    }
    
    @Test public void testLikeEscaped() throws Exception {
    	String query = "Name like 'R%*\\%\\_' escape '\\'";
    	String expectedContextFilter = "(cn=R*\\2a%_)"; //$NON-NLS-1$
    	
    	helpTestLike(query, expectedContextFilter);
    }
    
    @Test(expected=TranslatorException.class) public void testLikeUnsupported() throws Exception {
    	String query = "Name like 'R*_'";
    	String expectedContextFilter = null;
    	
    	helpTestLike(query, expectedContextFilter);
    }
    
    @Test(expected=TranslatorException.class) public void testLikeUnsupported1() throws Exception {
    	String query = "Name like 'R\\%_' escape '\\'";
    	String expectedContextFilter = null;
    	
    	helpTestLike(query, expectedContextFilter);
    }

	private void helpTestLike(String query, String expectedContextFilter)
			throws TranslatorException {
		LDAPSearchDetails searchDetails = helpGetSearchDetails("SELECT UserID FROM LdapModel.People WHERE " + query); //$NON-NLS-1$
        
        // Set Expected SearchDetails Values
        //-----------------------------------
        String expectedContextName = "ou=people,dc=metamatrix,dc=com"; //$NON-NLS-1$
        
        
        List<String> expectedAttrNameList = new ArrayList<String>();
        expectedAttrNameList.add("uid"); //$NON-NLS-1$
        
        long expectedCountLimit = -1;
        int expectedSearchScope = SearchControls.ONELEVEL_SCOPE;
        SortKey[] expectedSortKeys = null;
        
        helpTestSearchDetails(searchDetails, expectedContextName, expectedContextFilter, expectedAttrNameList,
        		expectedCountLimit, expectedSearchScope, expectedSortKeys);
	}
}

