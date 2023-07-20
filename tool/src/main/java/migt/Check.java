package migt;

import com.jayway.jsonpath.JsonPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static migt.Check.CheckOps.IS_NOT_PRESENT;

/**
 * Check Object class. This object is used in Operations to check that a parameter or some text is in as specified.
 *
 * @author Matteo Bitussi
 */
public class Check extends Module {
    String what; // what to search
    CheckOps op; // the check operations
    CheckIn in; // the section over which to search
    String op_val;
    List<String> value_list;
    boolean isParamCheck; // specifies if what is declared in what is a parameter name
    String regex;
    boolean use_variable;

    public Check() {
        init();
    }

    /**
     * Instantiate a new Check object given its parsed JSONObject
     *
     * @param json_check the check as JSONObject
     * @throws ParsingException
     */
    public Check(JSONObject json_check) throws ParsingException {
        init();
        Iterator<String> keys = json_check.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            switch (key) {
                case "in":
                    this.in = CheckIn.fromString(json_check.getString("in"));
                    break;
                case "check param":
                    this.isParamCheck = true;
                    this.setWhat(json_check.getString("check param"));
                    break;
                case "check":
                    this.setWhat(json_check.getString("check"));
                    break;
                case "check regex":
                    regex = json_check.getString("check regex");
                    break;
                case "use variable":
                    use_variable = json_check.getBoolean("use variable");
                    break;
                case "is":
                    this.setOp(CheckOps.IS);
                    this.op_val = json_check.getString("is");
                    break;
                case "is not":
                    this.setOp(CheckOps.IS_NOT);
                    this.op_val = json_check.getString("is not");
                    break;
                case "contains":
                    this.setOp(CheckOps.CONTAINS);
                    this.op_val = json_check.getString("contains");
                    break;
                case "not contains":
                    this.setOp(CheckOps.NOT_CONTAINS);
                    this.op_val = json_check.getString("not contains");
                    break;
                case "is present":
                    this.op = json_check.getBoolean("is present") ? CheckOps.IS_PRESENT :
                            IS_NOT_PRESENT;
                    this.op_val = json_check.getBoolean("is present") ?
                            "is present" : "is not present";
                    break;
                case "is in":
                    this.op = CheckOps.IS_IN;
                    JSONArray jsonArr = json_check.getJSONArray("is in");
                    Iterator<Object> it = jsonArr.iterator();

                    while (it.hasNext()) {
                        String act_enc = (String) it.next();
                        value_list.add(act_enc);
                    }
                    break;
                case "is not in":
                    this.op = CheckOps.IS_NOT_IN;
                    JSONArray jsonArr2 = json_check.getJSONArray("is not in");
                    Iterator<Object> it2 = jsonArr2.iterator();

                    while (it2.hasNext()) {
                        String act_enc = (String) it2.next();
                        value_list.add(act_enc);
                    }
                    break;
            }
        }
    }

    public void init() {
        what = "";
        op_val = "";
        isParamCheck = false;
        regex = "";
        value_list = new ArrayList<>();
        use_variable = false;
    }

    public void loader(DecodeOperation_API api) {
        this.imported_api = api;
    }

    /**
     * Execute the check if it is http
     *
     * @param message
     * @param isRequest
     * @return
     * @throws ParsingException
     */
    private boolean execute_http(HTTPReqRes message,
                                 boolean isRequest) throws ParsingException {
        String msg_str = "";
        if (this.in == null) {
            throw new ParsingException("from tag in checks is null");
        }

        switch (this.in) {
            case URL:
                if (!isRequest) {
                    throw new ParsingException("Searching URL in response");
                }
                msg_str = message.getUrlHeader();
                break;
            case BODY:
                msg_str = new String(message.getBody(isRequest), StandardCharsets.UTF_8);
                break;
            case HEAD:
                msg_str = String.join("\r\n", message.getHeaders(isRequest));
                break;
            default:
                System.err.println("no valid \"in\" specified in check");
                return false;
        }

        if (msg_str.length() == 0) {
            return false;
        }

        // if a regex is present, execute it
        if (!regex.equals("")) {
            return execute_regex(msg_str);
        }

        if (this.isParamCheck) {
            try {
                Pattern p = this.in == CheckIn.URL ?
                        Pattern.compile("(?<=[?&]" + this.what + "=)[^\\n&]*") :
                        Pattern.compile("(?<=" + this.what + ":\\s?)[^\\n]*");
                Matcher m = p.matcher(msg_str);

                String val = "";
                if (m.find()) {
                    val = m.group();
                } else {
                    return false;
                }

                if (this.op == null && val.length() != 0) {
                    // if it passed all the splits without errors, the param is present, but no checks are specified
                    // so result is true
                    return true;
                }
                switch (this.op) {
                    case IS:
                        if (!this.op_val.equals(val)) {
                            return false;
                        }
                        break;
                    case IS_NOT:
                        if (this.op_val.equals(val)) {
                            return false;
                        }
                        break;
                    case CONTAINS:
                        if (!val.contains(this.op_val)) {
                            return false;
                        }
                        break;
                    case NOT_CONTAINS:
                        if (val.contains(this.op_val)) {
                            return false;
                        }
                        break;
                    case IS_PRESENT:
                        return true; // if it gets to this, the searched param is already found
                    case IS_NOT_PRESENT:
                        return false;
                    case IS_IN:
                        return value_list.contains(val); // TODO check
                    case IS_NOT_IN:
                        return !value_list.contains(val);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                //e.printStackTrace();
                if (this.op != null) {
                    if (this.op != IS_NOT_PRESENT) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            if (!msg_str.contains(this.what)) {
                if (this.op != null) {
                    return this.op == IS_NOT_PRESENT;
                } else {
                    return false;
                }
            } else {
                if (this.op != null) {
                    return this.op != IS_NOT_PRESENT;
                }
            }
        }
        return true;
    }

    /**
     * Execute the json version of the check
     *
     * @return the result of the execution //TODO: change to API
     * @throws ParsingException
     */
    private boolean execute_json() throws ParsingException {
        DecodeOperation_API tmp = ((DecodeOperation_API) this.imported_api);

        String j = "";

        switch (in) {
            case JWT_HEADER: {
                j = tmp.jwt_header;
                break;
            }
            case JWT_PAYLOAD: {
                j = tmp.jwt_payload;
                break;
            }
            case JWT_SIGNATURE: {
                j = tmp.jwt_signature;
                break;
            }
        }

        // if a regex is present, execute it
        if (!regex.equals("")) {
            return execute_regex(j);
        }

        String found = "";
        // https://github.com/json-path/JsonPath
        try {
            found = JsonPath.read(j, what);
        } catch (com.jayway.jsonpath.PathNotFoundException e) {
            applicable = true;
            return op == IS_NOT_PRESENT;
        }

        applicable = true; // at this point the path has been found so the check is applicable

        if (isParamCheck) {
            throw new ParsingException("Cannot execute a 'check param' in a json, please use 'check'");
        }

        switch (op) {
            case IS:
                return op_val.equals(found);
            case IS_NOT:
                return !op_val.equals(found);
            case CONTAINS:
                return found.contains(op_val);
            case NOT_CONTAINS:
                return !found.contains(op_val);
            case IS_PRESENT:
                return !found.equals("");
            case IS_NOT_PRESENT:
                return found.equals("");
            case IS_IN:
                return value_list.contains(found);
            case IS_NOT_IN:
                return !value_list.contains(found);
        }

        return false;
    }

    /**
     * Executes the given check
     *
     * @param message
     * @param isRequest
     * @return the result of the check (passed or not passed)
     */
    public boolean execute(HTTPReqRes message,
                           boolean isRequest,
                           GUI gui) throws ParsingException {

        if (use_variable) {
            // Substitute to the op_val variable (that contains the name), the value of the variable
            op_val = Tools.getVariableByName(op_val, gui).value;
        }
        // TODO: migrate to api
        result = execute_http(message, isRequest);
        return result;
        // TODO REMOVE CONTENT TYPE
    }

    public void execute(GUI gui) throws ParsingException {
        if (use_variable) {
            // Substitute to the op_val variable (that contains the name), the value of the variable
            op_val = Tools.getVariableByName(op_val, gui).value;
        }

        switch (((DecodeOperation_API) imported_api).type) {
            case JWT:
                result = execute_json();
                break;
            case NONE:
                break;
            //TODO
            case XML:
                //TODO
                break;
        }
    }

    public void setWhat(String what) {
        this.what = what;
    }

    public void setOp(CheckOps op) {
        this.op = op;
    }

    @Override
    public String toString() {
        return "check: " + what + (op == null ? "" : " " + op + ": " + op_val);
    }

    /**
     * Executes the regex of the check against the given input, and returns true if the regex found something.
     *
     * @param input the input text to check
     * @return true if the regex matches, false otherwise
     */
    private boolean execute_regex(String input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        applicable = true;

        String val = "";
        if (m.find()) {
            val = m.group();
        } else {
            return false;
        }
        // TODO: add is, isnot, .. ?

        return true;
    }

    /**
     * enum containing all the possible check operations
     */
    public enum CheckOps {
        IS,
        IS_NOT,
        CONTAINS,
        NOT_CONTAINS,
        IS_PRESENT,
        IS_NOT_PRESENT,
        IS_IN,
        IS_NOT_IN;

        /**
         * Function that given a String, returns the corresponding CheckOps enum's value
         *
         * @param input the input string
         * @return the CheckOps enum value
         * @throws ParsingException if the input string does not correspond to any of the possible check operations
         */
        public static CheckOps fromString(String input) throws ParsingException {
            if (input != null) {
                switch (input) {
                    case "is":
                        return IS;
                    case "is not":
                        return IS_NOT;
                    case "contains":
                        return CONTAINS;
                    case "not contains":
                        return NOT_CONTAINS;
                    case "is in":
                        return IS_IN;
                    case "is not in":
                        return IS_NOT_IN;
                    default:
                        throw new ParsingException("invalid check operation");
                }
            } else {
                throw new NullPointerException();
            }
        }
    }

    /**
     * Used in the Check operation, to specify where is the content to check.
     */
    public enum CheckIn {
        // standard message
        HEAD,
        BODY,
        URL,
        // jwt
        JWT_HEADER,
        JWT_PAYLOAD,
        JWT_SIGNATURE;

        public static CheckIn fromString(String input) throws ParsingException {
            if (input != null) {
                switch (input) {
                    case "head":
                        return HEAD;
                    case "body":
                        return BODY;
                    case "url":
                        return URL;
                    case "header":
                        return JWT_HEADER;
                    case "payload":
                        return JWT_PAYLOAD;
                    case "signature":
                        return JWT_SIGNATURE;
                    default:
                        throw new ParsingException("invalid in '" + input + "' for check");
                }
            } else {
                throw new NullPointerException();
            }
        }
    }
}
