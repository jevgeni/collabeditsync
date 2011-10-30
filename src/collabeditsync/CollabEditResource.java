package collabeditsync;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

public class CollabEditResource {
    HttpClient client;
    HttpContext context = new BasicHttpContext();
    Integer cuid;
    private final String code;

    public CollabEditResource(String code) {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(
        new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

        ClientConnectionManager cm = new ThreadSafeClientConnManager(schemeRegistry);
        client = new DefaultHttpClient(cm);


        this.code = code;
        CookieStore cookieStore = new BasicCookieStore();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void login() throws IOException {
        HttpGet get = new HttpGet("http://collabedit.com/" + code);
        HttpResponse response = client.execute(get, context);
        String body = IOUtils.toString(response.getEntity().getContent());
        response.getEntity().getContent().close();
        cuid = extractCuid(body);
        System.out.println("Received cuid: " + cuid);
    }

    Integer extractCuid(String body) {
        Pattern pattern = Pattern.compile("var cuid = (\\d*);");
        Matcher matcher = pattern.matcher(body);
        matcher.find();
        return matcher.groupCount() > 0 ? Integer.parseInt(matcher.group(1)) : null;
    }

    public void changeNick(String nick) {
        String url = "http://collabedit.com/change_nick";
        try {
            postTo(client, url, withParams("new_name", nick, "guid", code));
            System.out.println("Nick changed to " + nick);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsuccessfulResponseException e) {
            e.printStackTrace();
        }
    }

    private String postTo(HttpClient client, String url, List<NameValuePair> params) throws IOException, UnsuccessfulResponseException {
        // set up httppost
        UrlEncodedFormEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(params, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpPost post = new HttpPost(url);
        post.setEntity(entity);
        HttpResponse response = client.execute(post, context);
        if(response.getStatusLine().getStatusCode() != 200) {
            response.getEntity().getContent().close();
            throw new UnsuccessfulResponseException(response.getStatusLine());
        }

        String body = IOUtils.toString(response.getEntity().getContent());
        response.getEntity().getContent().close();
        return body;
    }

    private List<NameValuePair> withParams(String... nameValues) {
        if (nameValues.length % 2 != 0) throw new IllegalArgumentException("Name and values should be given in pairs!");

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for(int name = 0, value = 1; value < nameValues.length; name++, value++) {
            params.add(new BasicNameValuePair(nameValues[name], nameValues[value]));
        }

        return params;
    }

    public JSONObject waitForUpdate(Boolean resync) throws IOException, UnsuccessfulResponseException {
        if (cuid == null) throw new IllegalStateException("Cannot query as I don't have CUID yet!");

        final String url = "http://collabedit.com/ot/wait";
        String rest = postTo(client, url, withParams("guid", code, "cuid", cuid.toString(), "resync", resync.toString().toLowerCase()));

        return (JSONObject) JSONSerializer.toJSON(rest);
    }

    public void sendUpdate(String oldText, String newText) throws IOException, UnsuccessfulResponseException {
        List<JSONArray> ops = new ArrayList<JSONArray>();
        if (!oldText.isEmpty()) ops.add(deleteElement(oldText));
        if (!newText.isEmpty()) ops.add(insertElement(newText));

        JSONObject object = new JSONObject()
                .element("cuid", cuid)
                .element("parent_hash", DigestUtils.md5Hex(oldText))
                .element("result_hash", DigestUtils.md5Hex(newText))
                .element("ops", ops);

        System.out.println("Sending out: " + object);

        String rest = postTo(client, "http://collabedit.com/ot/post", withParams("op", object.toString()));
        System.out.println("Response " + rest);
        JSONObject json = (JSONObject) JSONSerializer.toJSON(rest);
        System.out.println("Json: " + json);
    }

    public void sendUpdate(int myOffset, CharSequence beforeText, CharSequence afterText, CharSequence newFullText) throws IOException, UnsuccessfulResponseException {
        List<JSONArray> ops = new ArrayList<JSONArray>();

        if (myOffset > 0) ops.add(indexElement(myOffset));
        if (beforeText.length() > 0) ops.add(deleteElement(beforeText));
        if (afterText.length() > 0) ops.add(insertElement(afterText));

        String oldFullText = getOldText(myOffset, beforeText, afterText, newFullText);

        int indexFromRight = oldFullText.length() - myOffset;
        if (indexFromRight > 0) ops.add(indexElement(indexFromRight));

        JSONObject object = new JSONObject()
                .element("cuid", cuid)
                .element("parent_hash", DigestUtils.md5Hex(oldFullText.toString()))
                .element("result_hash", DigestUtils.md5Hex(newFullText.toString()))
                .element("ops", ops);

        System.out.println("Sending out: " + object);

        String rest = postTo(client, "http://collabedit.com/ot/post", withParams("op", object.toString()));

        JSONObject json = (JSONObject) JSONSerializer.toJSON(rest);
        System.out.println("Response on update: " + json);
    }

    private String getOldText(int myOffset, CharSequence beforeText, CharSequence afterText, CharSequence newFullText) {
        CharSequence firstPart = newFullText.subSequence(0, myOffset);
        CharSequence secondPart = newFullText.subSequence(myOffset + afterText.length(), newFullText.length());

        StringBuilder oldFullText = new StringBuilder(firstPart);
        oldFullText.append(beforeText);
        oldFullText.append(secondPart);
        return oldFullText.toString();
    }

    private JSONArray insertElement(CharSequence text) {
        JSONArray insert = new JSONArray();
        insert.add(8); // insert code
        insert.add(text.toString());
        return insert;
    }

    private JSONArray deleteElement(CharSequence text) {
        JSONArray delete = new JSONArray();
        delete.add(9); // delete code
        delete.add(text.toString());
        return delete;
    }

    private JSONArray indexElement(int pos) {
        JSONArray index = new JSONArray();
        index.add(7); // index code
        index.add(pos);
        return index;
    }
}
