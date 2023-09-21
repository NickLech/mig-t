import migt.*;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Utils_Test {

    @Test
    @DisplayName("Testing build string with vars")
    void testBuildStringWithVars() throws ParsingException {
        List<Var> vars = new ArrayList<>();
        Var v = new Var();
        v.name = "questo";
        v.isMessage = false;
        v.value = "provona";
        vars.add(v);

        Var v2 = new Var();
        v2.name = "qualcosaltro";
        v2.isMessage = false;
        v2.value = "prova";
        vars.add(v2);

        String s = "test da aggiungere $questo$ e poi $qualcosaltro$";
        String res = "";
        try {
            res = Tools.buildStringWithVars(vars, s);
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        assertEquals("test da aggiungere provona e poi prova", res);

        s = "open | https://$var3$$var4$ |";
        Var v3 = new Var();
        v3.name = "var3";
        v3.isMessage = false;
        v3.value = "www.youtube.com";
        vars.add(v3);

        Var v4 = new Var();
        v4.name = "var4";
        v4.isMessage = false;
        v4.value = "/link/a/caso";
        vars.add(v4);

        res = Tools.buildStringWithVars(vars, s);
        assertEquals("open | https://www.youtube.com/link/a/caso |", res);
    }

    @Test
    @DisplayName("Test find parent div")
    void testFindParentDiv() throws ParsingException {
        String in = "xpath=/html/body/div[3]/div[2]/div/div/div/div/div[3]/*[2]";
        String res = Tools.findParentDiv(in);
        assertEquals("xpath=/html/body/div[3]/div[2]/div/div/div/div/div", res);

        assertThrows(ParsingException.class, () -> {
            Tools.findParentDiv("https://www.facebook.com/");
        });

        assertThrows(ParsingException.class, () -> {
            Tools.findParentDiv("https://www.ted.com/settings/account");
        });

        in = "xpath=/html/body/div[2]/div/div[2]/form/div/span/span/button";
        res = Tools.findParentDiv(in);
        assertEquals("xpath=/html/body/div[2]/div/div[2]/form/div", res);

        in = "id=email";
        res = Tools.findParentDiv(in);
        assertEquals("id=email", res);
    }

    /*
    @Test
    @DisplayName("Test execute session ops")
    void testExecuteSessionOps() throws ParsingException {
        migt.Test t = new migt.Test();
        Operation op = new Operation();
        List<Var> vars = new ArrayList<>();

        t.sessions.add(new Session());

        SessionOperation s = new SessionOperation();
        s.action = Utils.SessOperationAction.SAVE;
        s.target = Utils.SessOperationTarget.LAST_CLICK;
        s.from_session = "s1";

        op.session_operations.add(s);

        t.last_click = new SessionTrackAction("click | xpath=qualcosa |");

        Object[] res = Utils.executeSessionOps(t, op, vars);
        op = (Operation) res[0];
        vars = (List<Var>) res[1];

        assertEquals("click | xpath=qualcosa |", vars.get(1).value);
    }
    */

    @Test
    @DisplayName("Test execute session Ops")
    void executeSessioOps_test() throws ParsingException {
        migt.Test t = new migt.Test();
        Session s = new Session("s1");

        try {
            s.setTrackFromString("set var | idp_usr | provaprovona99@outlook.it\n" +
                    "set var | idp_pw | Xflo98!@ops\n" +
                    "open | https://auth.fandom.com/signin |\n" +
                    "click | xpath=/html/body/div[2]/div/div/div[2]/div[2] |\n" +
                    "type | xpath=/html/body/div/main/div/div[2]/div/form[1]/section/div[1]/div/input | provaprovona\n" +
                    "wait | 500\n" +
                    "type | xpath=/html/body/div/main/div/div[2]/div/form[1]/section/div[2]/div[1]/div/input | Asddasdda123!\n" +
                    "wait | 300\n" +
                    "click | xpath=/html/body/div/main/div/div[2]/div/form[1]/section/div[3]/button |\n" +
                    "open | https://auth.fandom.com/auth/settings |\n" +
                    "click | xpath=/html/body/div[1]/main/div/div[2]/form/section[2]/div[2]/button[1] |\n" +
                    "click | xpath=/html/body/div[3]/div[2]/div/div/div/div/div[3]/button[2] |\n" +
                    "click | id=email |\n" +
                    "type | id=email | provaprovona99@outlook.it\n" +
                    "click | id=pass |\n" +
                    "type | id=pass | Xflo98!@ops\n" +
                    "click | id=loginbutton |\n" +
                    "click | xpath=/html/body/div[1]/div/div/div/div/div/div/div/div[1]/div/div/div[2]/div[2]/div[1]/div/div |\n" +
                    "assert open | https://auth.fandom.com/auth/settings |\n" +
                    "assert element content has | xpath=/html/body/div[1]/main/div/div[2]/form/section[2]/div[2]/button[1]/span[2] | Connect\n" +
                    "open | https://auth.fandom.com/auth/settings |\n" +
                    "click | xpath=/html/body/div[1]/main/div/div[2]/form/section[2]/div[2]/button[1] |");
        } catch (ParsingException e) {
            assertEquals(1, 0);
        }
        t.sessions.add(s);
        Operation op = new Operation();
        JSONObject sop_json = new JSONObject("{\"session operations\":[\n" +
                "  {\n" +
                "      \"session\": \"s1\",\n" +
                "      \"mark\": \"all_assert\",\n" +
                "      \"name\": \"A\"\n" +
                "  },\n" +
                "  {\n" +
                "      \"session\": \"s1\",\n" +
                "      \"remove\": \"track\",\n" +
                "      \"range\": \"[A,ML]\"\n" +
                "  }   \n" +
                "]}");
        try {
            op.session_operations = SessionOperation.parseFromJson(sop_json);
        } catch (ParsingException e) {
            assertEquals(1, 0);
        }

        List<Var> vars = new ArrayList<>();
        try {
            op.executeSessionOps(t, vars);
        } catch (ParsingException e) {
            assertEquals(1, 0);
        }

        Session right_output = new Session("s1");
        try {
            right_output.setTrackFromString("set var | idp_usr | provaprovona99@outlook.it\n" +
                    "set var | idp_pw | Xflo98!@ops\n" +
                    "open | https://auth.fandom.com/signin |\n" +
                    "click | xpath=/html/body/div[2]/div/div/div[2]/div[2] |\n" +
                    "type | xpath=/html/body/div/main/div/div[2]/div/form[1]/section/div[1]/div/input | provaprovona\n" +
                    "wait | 500\n" +
                    "type | xpath=/html/body/div/main/div/div[2]/div/form[1]/section/div[2]/div[1]/div/input | Asddasdda123!\n" +
                    "wait | 300\n" +
                    "click | xpath=/html/body/div/main/div/div[2]/div/form[1]/section/div[3]/button |\n" +
                    "open | https://auth.fandom.com/auth/settings |\n" +
                    "click | xpath=/html/body/div[1]/main/div/div[2]/form/section[2]/div[2]/button[1] |\n" +
                    "click | xpath=/html/body/div[3]/div[2]/div/div/div/div/div[3]/button[2] |\n" +
                    "click | id=email |\n" +
                    "type | id=email | provaprovona99@outlook.it\n" +
                    "click | id=pass |\n" +
                    "type | id=pass | Xflo98!@ops\n" +
                    "click | id=loginbutton |\n" +
                    "click | xpath=/html/body/div[1]/div/div/div/div/div/div/div/div[1]/div/div/div[2]/div[2]/div[1]/div/div |\n");
        } catch (ParsingException e) {
            assertEquals(1, 0);
        }

        try {
            assertEquals(t.getSession("s1").getTrack().toString(), right_output.getTrack().toString());
        } catch (ParsingException e) {
            assertEquals(1, 0);
        }
    }

    @Test
    void test_check_json_strings_equals() {
        boolean res = Tools.check_json_strings_equals(
                "{a : {a : 2}, b : 2}",
                "{b : 2, a : {a : 2}}"
        );

        assertEquals(true, res);
    }
}
