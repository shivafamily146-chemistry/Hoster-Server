const $ = (id) => document.getElementById(id);
const loginView = $("loginView"), dashboardView = $("dashboardView");
function notice(message) { $("notice").textContent = message; $("notice").classList.remove("hidden"); }
async function api(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { "Content-Type": "application/json", ...(options.headers || {}) } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || "Request failed.");
  return data;
}
function showDashboard(visible) {
  loginView.classList.toggle("hidden", visible);
  dashboardView.classList.toggle("hidden", !visible);
  $("logout").classList.toggle("hidden", !visible);
}
async function loadServers() {
  $("servers").innerHTML = "<p class='muted'>Loading servers…</p>";
  $("notice").classList.add("hidden");
  try {
    const servers = await api("/api/servers");
    $("servers").innerHTML = "";
    if (!servers.length) { $("servers").innerHTML = "<p class='muted'>No servers found on this panel account.</p>"; return; }
    servers.forEach(({ attributes: s }) => {
      const card = document.createElement("article"); card.className = "server-card";
      const name = document.createElement("h2"); name.textContent = s.name || s.identifier;
      const id = document.createElement("div"); id.className = "server-id"; id.textContent = "ID: " + s.identifier;
      const head = document.createElement("div"); head.className = "server-head";
      const title = document.createElement("div"); title.append(name,id);
      const status = document.createElement("span"); status.className = "status"; status.textContent = "Checking";
      head.append(title,status);
      const stats = document.createElement("div"); stats.className = "stats"; stats.innerHTML = "<div><b>—</b><span>CPU</span></div><div><b>—</b><span>MEMORY</span></div><div><b>—</b><span>DISK</span></div>";
      const actions = document.createElement("div"); actions.className = "actions";
      ["start","restart","stop"].forEach(signal => {
        const button = document.createElement("button"); button.textContent = signal[0].toUpperCase()+signal.slice(1);
        button.addEventListener("click", async () => {
          button.disabled = true;
          try { await api("/api/servers/"+encodeURIComponent(s.identifier)+"/power",{method:"POST",body:JSON.stringify({signal})}); notice("Sent "+signal+" command to "+s.name+"."); setTimeout(loadServers,1800); }
          catch(e) { notice(e.message); button.disabled = false; }
        });
        actions.append(button);
      });
      card.append(head,stats,actions); $("servers").append(card);
      api("/api/servers/"+encodeURIComponent(s.identifier)+"/resources").then(r => {
        const a=r.attributes||{}; const state=a.current_state||"unknown"; status.textContent=state; status.classList.toggle("running",state==="running");
        const m=a.resources||{}; const values=[(m.cpu_absolute??0).toFixed(1)+"%",Math.round((m.memory_bytes||0)/1048576)+" MB",Math.round((m.disk_bytes||0)/1048576)+" MB"];
        [...stats.children].forEach((el,i)=>el.querySelector("b").textContent=values[i]);
      }).catch(()=>{status.textContent="Unavailable";});
    });
  } catch(e) { $("servers").innerHTML=""; notice(e.message); }
}
$("loginForm").addEventListener("submit", async e => {
  e.preventDefault(); $("loginError").textContent="";
  try { await api("/api/login",{method:"POST",body:JSON.stringify({password:$("password").value})}); $("password").value=""; showDashboard(true); loadServers(); }
  catch(err) { $("loginError").textContent=err.message; }
});
$("logout").addEventListener("click",async()=>{await api("/api/logout",{method:"POST"}).catch(()=>{});showDashboard(false);});
$("refresh").addEventListener("click",loadServers);
api("/api/me").then(data=>{showDashboard(data.loggedIn);if(data.loggedIn)loadServers();}).catch(()=>showDashboard(false));
