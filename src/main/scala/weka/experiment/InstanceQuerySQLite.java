/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    InstanceQuery.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.experiment;

import org.sqlite.SQLiteConnection;
import weka.core.*;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.util.*;

/**
 * Convert the results of a database query into instances. The jdbc driver and
 * database to be used default to "jdbc.idbDriver" and
 * "jdbc:idb=experiments.prp". These may be changed by creating a java
 * properties file called DatabaseUtils.props in user.home or the current
 * directory. eg:
 * <p>
 * <p>
 * <code><pre>
 * jdbcDriver=jdbc.idbDriver
 * jdbcURL=jdbc:idb=experiments.prp
 * </pre></code>
 * <p>
 * <p>
 * Command line use just outputs the instances to System.out.
 * <p>
 * <p>
 * <!-- options-start --> Valid options are:
 * <p>
 * <p>
 * <pre>
 * -Q &lt;query&gt;
 *  SQL query to execute.
 * </pre>
 * <p>
 * <pre>
 * -S
 *  Return sparse rather than normal instances.
 * </pre>
 * <p>
 * <pre>
 * -U &lt;username&gt;
 *  The username to use for connecting.
 * </pre>
 * <p>
 * <pre>
 * -P &lt;password&gt;
 *  The password to use for connecting.
 * </pre>
 * <p>
 * <pre>
 * -D
 *  Enables debug output.
 * </pre>
 * <p>
 * <!-- options-end -->
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 10203 $
 */
public class InstanceQuerySQLite extends DatabaseUtils implements weka.core.OptionHandler,
        InstanceQueryAdapter {

    /**
     * for serialization
     */
    static final long serialVersionUID = 718158370917782584L;

    /**
     * Determines whether sparse data is created
     */
    protected boolean m_CreateSparseData = false;

    /**
     * Query to execute
     */
    protected String m_Query = "SELECT * from ?";

    /**
     * the custom props file to use instead of default one.
     */
    protected File m_CustomPropsFile = null;

    /**
     * Sets up the database drivers
     *
     * @throws Exception if an error occurs
     */
    public InstanceQuerySQLite() throws Exception {

        super();
    }

    public static weka.core.Instances retrieveInstances(InstanceQueryAdapter adapter,
                                                        ResultSet rs) throws Exception {
        if (adapter.getDebug()) {
            System.err.println("Getting metadata...");
        }
        ResultSetMetaData md = rs.getMetaData();
        if (adapter.getDebug()) {
            System.err.println("Completed getting metadata...");
        }

        // Determine structure of the instances
        int numAttributes = md.getColumnCount();
        int[] attributeTypes = new int[numAttributes];
        @SuppressWarnings("unchecked")
        Hashtable<String, Double>[] nominalIndexes = new Hashtable[numAttributes];
        @SuppressWarnings("unchecked")
        ArrayList<String>[] nominalStrings = new ArrayList[numAttributes];
        for (int i = 1; i <= numAttributes; i++) {
      /*
       * switch (md.getColumnType(i)) { case Types.CHAR: case Types.VARCHAR:
       * case Types.LONGVARCHAR: case Types.BINARY: case Types.VARBINARY: case
       * Types.LONGVARBINARY:
       */

            switch (adapter.translateDBColumnType(md.getColumnTypeName(i))) {

                case STRING:
                    // System.err.println("String --> nominal");
                    attributeTypes[i - 1] = weka.core.Attribute.NOMINAL;
                    nominalIndexes[i - 1] = new Hashtable<String, Double>();
                    nominalStrings[i - 1] = new ArrayList<String>();
                    break;
                case TEXT:
                    // System.err.println("Text --> string");
                    attributeTypes[i - 1] = weka.core.Attribute.STRING;
                    nominalIndexes[i - 1] = new Hashtable<String, Double>();
                    nominalStrings[i - 1] = new ArrayList<String>();
                    break;
                case BOOL:
                    // System.err.println("boolean --> nominal");
                    attributeTypes[i - 1] = weka.core.Attribute.NOMINAL;
                    nominalIndexes[i - 1] = new Hashtable<String, Double>();
                    nominalIndexes[i - 1].put("false", new Double(0));
                    nominalIndexes[i - 1].put("true", new Double(1));
                    nominalStrings[i - 1] = new ArrayList<String>();
                    nominalStrings[i - 1].add("false");
                    nominalStrings[i - 1].add("true");
                    break;
                case DOUBLE:
                    // System.err.println("BigDecimal --> numeric");
                    attributeTypes[i - 1] = weka.core.Attribute.NUMERIC;
                    break;
                case BYTE:
                    // System.err.println("byte --> numeric");
                    attributeTypes[i - 1] = weka.core.Attribute.NUMERIC;
                    break;
                case SHORT:
                    // System.err.println("short --> numeric");
                    attributeTypes[i - 1] = weka.core.Attribute.NUMERIC;
                    break;
                case INTEGER:
                    // System.err.println("int --> numeric");
                    attributeTypes[i - 1] = weka.core.Attribute.NUMERIC;
                    break;
                case LONG:
                    // System.err.println("long --> numeric");
                    attributeTypes[i - 1] = weka.core.Attribute.NUMERIC;
                    break;
                case FLOAT:
                    // System.err.println("float --> numeric");
                    attributeTypes[i - 1] = weka.core.Attribute.NUMERIC;
                    break;
                case DATE:
                    attributeTypes[i - 1] = weka.core.Attribute.DATE;
                    break;
                case TIME:
                    attributeTypes[i - 1] = weka.core.Attribute.DATE;
                    break;
                default:
                    // System.err.println("Unknown column type");
                    attributeTypes[i - 1] = weka.core.Attribute.STRING;
            }
        }

        // For sqlite
        // cache column names because the last while(rs.next()) { iteration for
        // the tuples below will close the md object:
        Vector<String> columnNames = new Vector<String>();
        for (int i = 0; i < numAttributes; i++) {
            columnNames.add(md.getColumnLabel(i + 1));
        }

        // Step through the tuples
        if (adapter.getDebug()) {
            System.err.println("Creating instances...");
        }
        ArrayList<weka.core.Instance> instances = new ArrayList<weka.core.Instance>();
        int rowCount = 0;
        while (rs.next()) {
            if (rowCount % 100 == 0) {
                if (adapter.getDebug()) {
                    System.err.print("read " + rowCount + " instances \r");
                    System.err.flush();
                }
            }
            double[] vals = new double[numAttributes];
            for (int i = 1; i <= numAttributes; i++) {
        /*
         * switch (md.getColumnType(i)) { case Types.CHAR: case Types.VARCHAR:
         * case Types.LONGVARCHAR: case Types.BINARY: case Types.VARBINARY: case
         * Types.LONGVARBINARY:
         */
                switch (adapter.translateDBColumnType(md.getColumnTypeName(i))) {
                    case STRING:
                        String str = rs.getString(i);

                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            Double index = nominalIndexes[i - 1].get(str);
                            if (index == null) {
                                index = new Double(nominalStrings[i - 1].size());
                                nominalIndexes[i - 1].put(str, index);
                                nominalStrings[i - 1].add(str);
                            }
                            vals[i - 1] = index.doubleValue();
                        }
                        break;
                    case TEXT:
                        String txt = rs.getString(i);

                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            Double index = nominalIndexes[i - 1].get(txt);
                            if (index == null) {

                                // Need to add one because first value in
                                // string attribute is dummy value.
                                index = new Double(nominalStrings[i - 1].size()) + 1;
                                nominalIndexes[i - 1].put(txt, index);
                                nominalStrings[i - 1].add(txt);
                            }
                            vals[i - 1] = index.doubleValue();
                        }
                        break;
                    case BOOL:
                        boolean boo = rs.getBoolean(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            vals[i - 1] = (boo ? 1.0 : 0.0);
                        }
                        break;
                    case DOUBLE:
                        // BigDecimal bd = rs.getBigDecimal(i, 4);
                        double dd = rs.getDouble(i);
                        // Use the column precision instead of 4?
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            // newInst.setValue(i - 1, bd.doubleValue());
                            vals[i - 1] = dd;
                        }
                        break;
                    case BYTE:
                        byte by = rs.getByte(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            vals[i - 1] = by;
                        }
                        break;
                    case SHORT:
                        short sh = rs.getShort(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            vals[i - 1] = sh;
                        }
                        break;
                    case INTEGER:
                        int in = rs.getInt(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            vals[i - 1] = in;
                        }
                        break;
                    case LONG:
                        long lo = rs.getLong(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            vals[i - 1] = lo;
                        }
                        break;
                    case FLOAT:
                        float fl = rs.getFloat(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            vals[i - 1] = fl;
                        }
                        break;
                    case DATE:
                        Date date = rs.getDate(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            // TODO: Do a value check here.
                            vals[i - 1] = date.getTime();
                        }
                        break;
                    case TIME:
                        Time time = rs.getTime(i);
                        if (rs.wasNull()) {
                            vals[i - 1] = weka.core.Utils.missingValue();
                        } else {
                            // TODO: Do a value check here.
                            vals[i - 1] = time.getTime();
                        }
                        break;
                    default:
                        vals[i - 1] = weka.core.Utils.missingValue();
                }
            }
            weka.core.Instance newInst;
            if (adapter.getSparseData()) {
                newInst = new weka.core.SparseInstance(1.0, vals);
            } else {
                newInst = new DenseInstance(1.0, vals);
            }
            instances.add(newInst);
            rowCount++;
        }
        // disconnectFromDatabase(); (perhaps other queries might be made)

        // Create the header and add the instances to the dataset
        if (adapter.getDebug()) {
            System.err.println("Creating header...");
        }
        ArrayList<weka.core.Attribute> attribInfo = new ArrayList<weka.core.Attribute>();
        for (int i = 0; i < numAttributes; i++) {
      /* Fix for databases that uppercase column names */
            // String attribName = attributeCaseFix(md.getColumnName(i + 1));
            String attribName = adapter.attributeCaseFix(columnNames.get(i));
            switch (attributeTypes[i]) {
                case weka.core.Attribute.NOMINAL:
                    attribInfo.add(new weka.core.Attribute(attribName, nominalStrings[i]));
                    break;
                case weka.core.Attribute.NUMERIC:
                    attribInfo.add(new weka.core.Attribute(attribName));
                    break;
                case weka.core.Attribute.STRING:
                    weka.core.Attribute att = new weka.core.Attribute(attribName, (ArrayList<String>) null);
                    attribInfo.add(att);
                    for (int n = 0; n < nominalStrings[i].size(); n++) {
                        att.addStringValue(nominalStrings[i].get(n));
                    }
                    break;
                case weka.core.Attribute.DATE:
                    attribInfo.add(new weka.core.Attribute(attribName, (String) null));
                    break;
                default:
                    throw new Exception("Unknown attribute type");
            }
        }
        weka.core.Instances result = new weka.core.Instances("QueryResult", attribInfo,
                instances.size());
        for (int i = 0; i < instances.size(); i++) {
            result.add(instances.get(i));
        }

        return result;
    }

    /**
     * Test the class from the command line. The instance query should be
     * specified with -Q sql_query
     *
     * @param args contains options for the instance query
     */
    public static void main(String args[]) {

        try {
            InstanceQuerySQLite iq = new InstanceQuerySQLite();
            String query = weka.core.Utils.getOption('Q', args);
            if (query.length() == 0) {
                iq.setQuery("select * from Experiment_index");
            } else {
                iq.setQuery(query);
            }
            iq.setOptions(args);
            try {
                weka.core.Utils.checkForRemainingOptions(args);
            } catch (Exception e) {
                System.err.println("Options for weka.experiment.InstanceQuery:\n");
                Enumeration<weka.core.Option> en = iq.listOptions();
                while (en.hasMoreElements()) {
                    weka.core.Option o = en.nextElement();
                    System.err.println(o.synopsis() + "\n" + o.description());
                }
                System.exit(1);
            }

            weka.core.Instances aha = iq.retrieveInstances();
            iq.disconnectFromDatabase();
            // query returned no result -> exit
            if (aha == null) {
                return;
            }
            // The dataset may be large, so to make things easier we'll
            // output an instance at a time (rather than having to convert
            // the entire dataset to one large string)
            System.out.println(new weka.core.Instances(aha, 0));
            for (int i = 0; i < aha.numInstances(); i++) {
                System.out.println(aha.instance(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    /**
     * Returns an enumeration describing the available options
     * <p>
     *
     * @return an enumeration of all options
     */
    @Override
    public Enumeration<weka.core.Option> listOptions() {
        Vector<weka.core.Option> result = new Vector<weka.core.Option>();

        result.addElement(new weka.core.Option("\tSQL query to execute.", "Q", 1,
                "-Q <query>"));

        result.addElement(new weka.core.Option(
                "\tReturn sparse rather than normal instances.", "S", 0, "-S"));

        result.addElement(new weka.core.Option("\tThe username to use for connecting.", "U",
                1, "-U <username>"));

        result.addElement(new weka.core.Option("\tThe password to use for connecting.", "P",
                1, "-P <password>"));

        result.add(new weka.core.Option(
                "\tThe custom properties file to use instead of default ones,\n"
                        + "\tcontaining the database parameters.\n" + "\t(default: none)",
                "custom-props", 1, "-custom-props <file>"
        ));

        result.addElement(new weka.core.Option("\tEnables debug output.", "D", 0, "-D"));

        return result.elements();
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String queryTipText() {
        return "The SQL query to execute against the database.";
    }

    /**
     * Get the query to execute against the database
     *
     * @return the query
     */
    public String getQuery() {
        return m_Query;
    }

    /**
     * Set the query to execute against the database
     *
     * @param q the query to execute
     */
    public void setQuery(String q) {
        m_Query = q;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String sparseDataTipText() {
        return "Encode data as sparse instances.";
    }

    /**
     * Gets whether data is to be returned as a set of sparse instances
     *
     * @return true if data is to be encoded as sparse instances
     */
    @Override
    public boolean getSparseData() {
        return m_CreateSparseData;
    }

    /**
     * Sets whether data should be encoded as sparse instances
     *
     * @param s true if data should be encoded as a set of sparse instances
     */
    public void setSparseData(boolean s) {
        m_CreateSparseData = s;
    }

    /**
     * Returns the custom properties file in use, if any.
     *
     * @return the custom props file, null if none used
     */
    public File getCustomPropsFile() {
        return m_CustomPropsFile;
    }

    /**
     * Sets the custom properties file to use.
     *
     * @param value the custom props file to load database parameters from, use
     *              null or directory to disable custom properties.
     * @see #initialize(java.io.File)
     */
    public void setCustomPropsFile(File value) {
        m_CustomPropsFile = value;
        initialize(m_CustomPropsFile);
    }

    /**
     * The tip text for this property.
     *
     * @return the tip text
     */
    public String customPropsFileTipText() {
        return "The custom properties that the user can use to override the default ones.";
    }

    /**
     * Gets the current settings of InstanceQuery
     *
     * @return an array of strings suitable for passing to setOptions()
     */
    @Override
    public String[] getOptions() {

        Vector<String> options = new Vector<String>();

        options.add("-Q");
        options.add(getQuery());

        if (getSparseData()) {
            options.add("-S");
        }

        if (!getUsername().equals("")) {
            options.add("-U");
            options.add(getUsername());
        }

        if (!getPassword().equals("")) {
            options.add("-P");
            options.add(getPassword());
        }

        if ((m_CustomPropsFile != null) && !m_CustomPropsFile.isDirectory()) {
            options.add("-custom-props");
            options.add(m_CustomPropsFile.toString());
        }

        if (getDebug()) {
            options.add("-D");
        }

        return options.toArray(new String[options.size()]);
    }

    /**
     * Parses a given list of options.
     * <p>
     * <!-- options-start --> Valid options are:
     * <p>
     * <p>
     * <pre>
     * -Q &lt;query&gt;
     *  SQL query to execute.
     * </pre>
     * <p>
     * <pre>
     * -S
     *  Return sparse rather than normal instances.
     * </pre>
     * <p>
     * <pre>
     * -U &lt;username&gt;
     *  The username to use for connecting.
     * </pre>
     * <p>
     * <pre>
     * -P &lt;password&gt;
     *  The password to use for connecting.
     * </pre>
     * <p>
     * <pre>
     * -D
     *  Enables debug output.
     * </pre>
     * <p>
     * <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {

        String tmpStr;

        setSparseData(weka.core.Utils.getFlag('S', options));

        tmpStr = weka.core.Utils.getOption('Q', options);
        if (tmpStr.length() != 0) {
            setQuery(tmpStr);
        }

        tmpStr = weka.core.Utils.getOption('U', options);
        if (tmpStr.length() != 0) {
            setUsername(tmpStr);
        }

        tmpStr = weka.core.Utils.getOption('P', options);
        if (tmpStr.length() != 0) {
            setPassword(tmpStr);
        }

        tmpStr = weka.core.Utils.getOption("custom-props", options);
        if (tmpStr.length() == 0) {
            setCustomPropsFile(null);
        } else {
            setCustomPropsFile(new File(tmpStr));
        }

        setDebug(weka.core.Utils.getFlag('D', options));
    }

    /**
     * Makes a database query using the query set through the -Q option to convert
     * a table into a set of instances
     *
     * @return the instances contained in the result of the query
     * @throws Exception if an error occurs
     */
    public weka.core.Instances retrieveInstances() throws Exception {
        return retrieveInstances(m_Query);
    }

    /**
     * Makes a database query to convert a table into a set of instances
     *
     * @param query the query to convert to instances
     * @return the instances contained in the result of the query, NULL if the SQL
     * query doesn't return a ResultSet, e.g., DELETE/INSERT/UPDATE
     * @throws Exception if an error occurs
     */
    public weka.core.Instances retrieveInstances(String query) throws Exception {

        if (m_Debug) {
            System.err.println("Executing query: " + query);
        }
        connectToDatabase();
        ((SQLiteConnection) m_Connection).setBusyTimeout(20 * 60 * 1000); //20min. de timeout
        execute("attach 'app.db' as app");

        //eliminating id column from attributes
        if (!execute("select * from i limit 1")) {
            if (m_PreparedStatement.getUpdateCount() == -1) throw new Exception("Query didn't produce results");
            else {
                if (m_Debug) System.err.println(m_PreparedStatement.getUpdateCount() + " rows affected.");
                close();
                return null;
            }
        }
        ResultSet rs0 = getResultSet();
        ResultSetMetaData rsMetaData = rs0.getMetaData();
        int numberOfColumns = rsMetaData.getColumnCount();
        String str = "";
        for (int i = 2; i <= numberOfColumns; i++)
            str += rsMetaData.getColumnName(i) + ",";
        str = str.substring(0, str.length() - 1);
        System.out.println("select " + str + " from ( " + query + " )");
//        if (!execute("select " + str + " from ( " + query + " )")) {
        System.out.println(query);
        rs0.close();


        //original query
        if (!execute(query)) {
            if (m_PreparedStatement.getUpdateCount() == -1) {
                throw new Exception("Query didn't produce results");
            } else {
                if (m_Debug) {
                    System.err.println(m_PreparedStatement.getUpdateCount()
                            + " rows affected.");
                }
                close();
                return null;
            }
        }
        ResultSet rs = getResultSet();


        if (m_Debug) {
            System.err.println("Getting metadata...");
        }

        weka.core.Instances result = retrieveInstances(this, rs);
        close(rs);

        return result;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 10203 $");
    }
}
