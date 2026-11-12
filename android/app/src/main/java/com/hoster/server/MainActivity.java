package com.hoster.server;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private android.content.SharedPreferences prefs;
    private LinearLayout root, content;
    private EditText panelInput, tokenInput;
    private TextView status;
    private String panel, token;
    private int white = Color.WHITE, muted = Color.rgb(166,176,194), accent = Color.rgb(76,175,125);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12,18,30));
        getWindow().setNavigationBarColor(Color.rgb(12,18,30));
        prefs = getSharedPreferences("hoster", MODE_PRIVATE);
        panel = prefs.getString("panel", ""); token = prefs.getString("token", "");
        buildShell(); showSettings();
    }
    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,16); root.setBackgroundColor(Color.rgb(12,18,30));
        TextView brand = text("HOSTER SERVER", 23, white, true); root.addView(brand);
        TextView sub = text("Minecraft server control", 13, muted, false); root.addView(sub);
        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(0,18,0,12);
        Button dashboard = button("Dashboard"), settings = button("Settings");
        dashboard.setOnClickListener(v -> loadServers()); settings.setOnClickListener(v -> showSettings()); nav.addView(dashboard); nav.addView(settings); root.addView(nav);
        status = text("Not connected", 12, muted, false); root.addView(status);
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,14,0,20); scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    private TextView text(String value,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(0,5,0,5); return t; }
    private Button button(String value) { Button b=new Button(this); b.setText(value); b.setAllCaps(false); return b; }
    private void showSettings() {
        content.removeAllViews(); content.addView(text("Connect your panel",19,white,true));
        content.addView(text("Enter your Pterodactyl panel URL and Client API key. Use HTTPS.",13,muted,false));
        panelInput = new EditText(this); panelInput.setSingleLine(true); panelInput.setHint("https://panel.example.com"); panelInput.setText(panel); content.addView(panelInput);
        tokenInput = new EditText(this); tokenInput.setSingleLine(true); tokenInput.setHint("ptlc_... Client API key"); tokenInput.setInputType(129); tokenInput.setText(token); content.addView(tokenInput);
        Button save=button("Save and connect"); content.addView(save); save.setOnClickListener(v -> {
            String p=panelInput.getText().toString().trim().replaceAll("/+$", ""), k=tokenInput.getText().toString().trim();
            if(!p.startsWith("https://") || k.isEmpty()){ status.setText("Use a valid HTTPS panel URL and API key"); return; }
            panel=p; token=k; prefs.edit().putString("panel",panel).putString("token",token).apply(); loadServers();
        });
        content.addView(text("Security: the API key is stored on this device. Do not share it. The app needs a Pterodactyl panel and a Client API key; it does not create hosting infrastructure by itself.",12,muted,false));
    }
    private JSONObject request(String path,String method) throws Exception {
        URL u=new URL(panel+"/api/client"+path); HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(15000); c.setRequestProperty("Authorization","Bearer "+token); c.setRequestProperty("Accept","Application/vnd.pterodactyl.v1+json"); c.setRequestProperty("Content-Type","application/json");
        int code=c.getResponseCode(); BufferedReader r=new BufferedReader(new InputStreamReader(code>=200&&code<300?c.getInputStream():c.getErrorStream(),StandardCharsets.UTF_8)); StringBuilder out=new StringBuilder(); String line; while((line=r.readLine())!=null)out.append(line); r.close(); c.disconnect(); if(code<200||code>=300) throw new Exception("Panel API error "+code+": "+out); return new JSONObject(out.toString());
    }
    private void loadServers() {
        if(panel.isEmpty()||token.isEmpty()){ showSettings(); return; }
        status.setText("Connecting…"); content.removeAllViews(); content.addView(text("Your servers",19,white,true));
        io.execute(() -> { try { JSONObject data=request("/", "GET"); JSONArray arr=data.getJSONArray("data"); runOnUiThread(() -> { status.setText(arr.length()+" server(s) loaded"); content.removeAllViews(); content.addView(text("Your servers",19,white,true)); if(arr.length()==0) content.addView(text("No servers are assigned to this API account.",14,muted,false)); for(int i=0;i<arr.length();i++){ JSONObject item=arr.optJSONObject(i); JSONObject a=item.optJSONObject("attributes"); if(a!=null)addServerCard(a); } }); } catch(Exception e){ runOnUiThread(() -> { status.setText("Connection failed"); content.removeAllViews(); content.addView(text(e.getMessage(),13,Color.rgb(255,130,130),false)); Button openSettings = button("Open settings"); openSettings.setOnClickListener(x -> showSettings()); content.addView(openSettings); }); } });
    }
    private void addServerCard(JSONObject a) {
        String id=a.optString("identifier"), name=a.optString("name"), state=a.optString("status","Unknown");
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(16,14,16,14); card.setBackgroundColor(Color.rgb(25,35,52)); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=12; content.addView(card,cp);
        card.addView(text(name,17,white,true)); card.addView(text("ID: "+id+"  •  "+state,12,muted,false));
        LinearLayout actions=new LinearLayout(this); Button start=button("Start"), stop=button("Stop"), restart=button("Restart"), refresh=button("Refresh"); actions.addView(start); actions.addView(stop); actions.addView(restart); actions.addView(refresh); card.addView(actions);
        start.setOnClickListener(v -> power(id,"start")); stop.setOnClickListener(v -> power(id,"stop")); restart.setOnClickListener(v -> power(id,"restart")); refresh.setOnClickListener(v -> loadServers());
    }
    private void power(String id,String signal) { status.setText("Sending "+signal+"…"); io.execute(() -> { try { URL u=new URL(panel+"/api/client/servers/"+id+"/power"); HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(15000); c.setRequestProperty("Authorization","Bearer "+token); c.setRequestProperty("Accept","Application/vnd.pterodactyl.v1+json"); c.setRequestProperty("Content-Type","application/json"); c.getOutputStream().write(("{\"signal\":\""+signal+"\"}").getBytes(StandardCharsets.UTF_8)); int code=c.getResponseCode(); c.disconnect(); if(code<200||code>=300)throw new Exception("Power action failed: HTTP "+code); runOnUiThread(() -> status.setText(signal+" command sent")); } catch(Exception e){ runOnUiThread(() -> status.setText(e.getMessage())); } }); }
    @Override protected void onDestroy(){ super.onDestroy(); io.shutdownNow(); }
}
