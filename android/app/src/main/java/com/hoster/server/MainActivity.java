package com.hoster.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private android.content.SharedPreferences prefs;
    private LinearLayout root, content;
    private TextView status;
    private final int bg=Color.rgb(12,18,30), panel=Color.rgb(25,35,52), white=Color.WHITE, muted=Color.rgb(166,176,194), accent=Color.rgb(76,175,125);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        prefs=getSharedPreferences("hoster_bedrock",MODE_PRIVATE);
        buildShell(); showHome();
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(0,5,0,5); return t;
    }
    private Button button(String s) { Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private void buildShell() {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,16); root.setBackgroundColor(bg);
        root.addView(text("HOSTER SERVER",24,white,true));
        root.addView(text("FREE BEDROCK HOSTING • NO VPS",12,accent,true));
        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        Button home=button("Home"), add=button("Add Server"); home.setOnClickListener(v->showHome()); add.setOnClickListener(v->showAddDialog());
        nav.addView(home,new LinearLayout.LayoutParams(0,-2,1)); nav.addView(add,new LinearLayout.LayoutParams(0,-2,1)); root.addView(nav);
        status=text("Ready",12,muted,false); root.addView(status);
        ScrollView scroll=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,12,0,24); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }
    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch(Exception e) { status.setText("Cannot open browser: "+e.getMessage()); }
    }
    private void showHome() {
        content.removeAllViews();
        content.addView(text("Your Bedrock servers",20,white,true));
        content.addView(text("No VPS is needed for this app setup. Create a free server with a provider, then save its address and port here.",14,muted,false));
        addProvider("Aternos — Free Bedrock hosting","Create a free Bedrock server on Aternos. Start it on the Aternos website, then copy the address and port into this app.","https://aternos.org/");
        addProvider("Minehut — Free Java/Bedrock network","Create a free Minehut server. Bedrock mobile players can join through the Minehut Bedrock address; this is a shared network, not a standalone Bedrock-only server.","https://www.minehut.com/");
        Button add=button("＋ Add my server IP / Port"); add.setOnClickListener(v->showAddDialog()); content.addView(add);
        content.addView(text("Saved servers",18,white,true));
        JSONArray arr=readServers();
        if(arr.length()==0) content.addView(text("No saved servers yet.",13,muted,false));
        for(int i=0;i<arr.length();i++) { JSONObject item=arr.optJSONObject(i); if(item!=null) addServerCard(item); }
        content.addView(text("Important: Hoster Server currently saves connection details and opens provider websites. It cannot create, start, or keep a Minecraft server running by itself. Free providers control server uptime and availability.",12,muted,false));
    }
    private void addProvider(String title,String desc,String url) {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(15,12,15,12); card.setBackgroundColor(panel);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=12; content.addView(card,lp);
        card.addView(text(title,17,white,true)); card.addView(text(desc,13,muted,false));
        Button open=button("Open provider website"); open.setOnClickListener(v->openUrl(url)); card.addView(open);
    }
    private JSONArray readServers() {
        try { return new JSONArray(prefs.getString("servers","[]")); } catch(Exception e) { return new JSONArray(); }
    }
    private void saveServers(JSONArray arr) { prefs.edit().putString("servers",arr.toString()).apply(); }
    private void showAddDialog() {
        LinearLayout form=new LinearLayout(this); form.setPadding(12,8,12,4); form.setOrientation(LinearLayout.VERTICAL);
        EditText name=new EditText(this); name.setSingleLine(true); name.setHint("Server name");
        EditText address=new EditText(this); address.setSingleLine(true); address.setHint("Server address / IP");
        EditText port=new EditText(this); port.setSingleLine(true); port.setInputType(2); port.setHint("Port (Bedrock default: 19132)");
        form.addView(name); form.addView(address); form.addView(port);
        new AlertDialog.Builder(this).setTitle("Add Bedrock server").setView(form)
          .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            String n=name.getText().toString().trim(), a=address.getText().toString().trim(), p=port.getText().toString().trim();
            if(n.isEmpty()||a.isEmpty()||p.isEmpty()){ status.setText("Enter server name, address and port."); return; }
            try { int pn=Integer.parseInt(p); if(pn<1||pn>65535)throw new Exception(); JSONArray arr=readServers(); JSONObject item=new JSONObject(); item.put("name",n); item.put("address",a); item.put("port",pn); arr.put(item); saveServers(arr); status.setText("Server saved on this phone."); showHome(); }
            catch(Exception e){ status.setText("Invalid port. Enter a number from 1 to 65535."); }
          }).show();
    }
    private void addServerCard(JSONObject item) {
        String name=item.optString("name","Minecraft Server"), address=item.optString("address",""), port=String.valueOf(item.optInt("port",19132));
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(15,12,15,12); card.setBackgroundColor(panel);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=12; content.addView(card,lp);
        card.addView(text(name,17,white,true)); card.addView(text("Address: "+address,14,white,false)); card.addView(text("Port: "+port,14,white,false));
        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button copy=button("Copy address"), join=button("Open Minecraft"), remove=button("Delete");
        actions.addView(copy,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(join,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(remove,new LinearLayout.LayoutParams(0,-2,1)); card.addView(actions);
        copy.setOnClickListener(v->{ ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText("Bedrock server",address+":"+port)); status.setText("Address and port copied."); });
        join.setOnClickListener(v->{ try { Intent launch=getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe"); if(launch==null)throw new Exception(); startActivity(launch); status.setText("Minecraft opened. Add this address in Play → Servers → Add Server."); } catch(Exception e){ status.setText("Install Minecraft Bedrock to join."); } });
        remove.setOnClickListener(v->{ JSONArray old=readServers(), next=new JSONArray(); for(int i=0;i<old.length();i++){ JSONObject x=old.optJSONObject(i); if(x!=null && !(x.optString("name").equals(name)&&x.optString("address").equals(address)&&String.valueOf(x.optInt("port")).equals(port)))next.put(x); } saveServers(next); showHome(); status.setText("Server removed."); });
    }
}
