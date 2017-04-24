/**
 *  Foscam HD
 *
 *  Author: skp19
 *  Date: 6/18/14
 *
 *  Example device type for HD Foscam cameras.
 *  Code based off of Foscam device type by brian@bevey.org.
 *  Heavily modified to work with the Foscam HD cameras.
 *
 *  This device has the following functions:
 *    - Take a snapshot
 *    - Toggle the infrared lights
 *    - Enable/Disable motion alarm
 *    - Go to and set preset locations
 *    - Enable cruise maps
 *    - Control PTZ
 *    - Reboot
 *
 *  Capability: Image Capture, Polling
 *  Custom Attributes: setStatus, alarmStatus, ledStatus
 *  Custom Commands: alarmOn, alarmOff, toggleAlarm, left, right, up, down,
 *                   stop, set, preset, preset1, preset2, preset3, cruisemap1,
 *                   cruisemap2, cruise, toggleLED, ledOn, ledOff, ledAuto
 */

preferences {
  input("username", "text",        title: "Username",                description: "Your Foscam camera username")
  input("password", "password",    title: "Password",                description: "Your Foscam camera password")
  input("ip",       "text",        title: "IP address/Hostname",     description: "The IP address or hostname of your Foscam camera")
  input("port",     "text",        title: "Port",                    description: "The port of your Foscam camera")  
}

metadata {
  definition (name: "Foscam HD") {
    capability "Polling"
    capability "Image Capture"

    attribute "setStatus",   "string"
    attribute "alarmStatus", "string"
	attribute "ledStatus",   "string"
    
    command "alarmOn"
    command "alarmOff"
    command "toggleAlarm"
    command "toggleLED"
    command "ledOn"
    command "ledOff"
    command "ledAuto"
    command "reboot"
  }

  tiles {
    carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

	  standardTile("foscam", "device.alarmStatus", width: 1, height: 1, canChangeIcon: true, inactiveLabel: true, canChangeBackground: false) {
      state "off", label: "off", action: "toggleAlarm", icon: "st.camera.dropcam-centered", backgroundColor: "#FFFFFF"
      state "on", label: "on", action: "toggleAlarm", icon: "st.camera.dropcam-centered",  backgroundColor: "#53A7C0"
    }
	
    standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "default", label: "", action: "Image Capture.take", icon: "st.camera.dropcam-centered", backgroundColor: "#FFFFFF"
    }

    standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
      state "take", label: "", action: "Image Capture.take", icon: "st.secondary.take", nextState:"taking"
    }
    
    standardTile("alarmStatus", "device.alarmStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "off", label: "off", action: "toggleAlarm", icon: "st.quirky.spotter.quirky-spotter-sound-off", backgroundColor: "#FFFFFF"
      state "on", label: "on", action: "toggleAlarm", icon: "st.quirky.spotter.quirky-spotter-sound-on",  backgroundColor: "#53A7C0"
    }

    standardTile("refresh", "device.alarmStatus", inactiveLabel: false, decoration: "flat") {
      state "refresh", action:"polling.poll", icon:"st.secondary.refresh"
    }

	  standardTile("ledStatus", "device.ledStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "auto", label: "auto", action: "toggleLED", icon: "st.Lighting.light13", backgroundColor: "#53A7C0"
	  state "off", label: "off", action: "toggleLED", icon: "st.Lighting.light13", backgroundColor: "#FFFFFF"
      state "on", label: "on", action: "toggleLED", icon: "st.Lighting.light11", backgroundColor: "#FFFF00"
	  state "manual", label: "manual", action: "toggleLED", icon: "st.Lighting.light13", backgroundColor: "#FFFF00"
    }
	
	  standardTile("reboot", "device.image", inactiveLabel: false, decoration: "flat") {
      state "reboot", label: "reboot", action: "reboot", icon: "st.Health & Wellness.health8"
    }
	
    main "foscam"
      details(["cameraDetails", "take", "ledStatus", "alarmStatus", "refresh", "reboot"])
  }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    // initialize counter
    log.debug("Initialize")
    state.waitingMethod = ""
    state.alarmData = [:]
    state.alarmEnabled = ""
}

private getPictureName() {
  def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
  "image" + "_$pictureUuid" + ".jpg"
}

def take() {
  log.debug("Take a photo")
  hubGet("snapPicture2",[:]) 
}

def toggleAlarm() {
  if(device.currentValue("alarmStatus") == "on") {
    alarmOff()
  } else {
    alarmOn()
  }
}

def alarmOn() {
/*
  state.alarmEnabled = "1"
  delayBetween([hubGet("getMotionDetectConfig",[:]), state.alarmData["isEnable"] = state.alarmEnabled, hubGet("setMotionDetectConfig", state.alarmData )], 100)
  sendEvent(name: "alarmStatus", value: "on");
  */
}

def alarmOff() {
/*
  state.alarmEnabled = "0"
  delayBetween([hubGet("getMotionDetectConfig",[:]), state.alarmData["isEnable"] = state.alarmEnabled, hubGet("setMotionDetectConfig", state.alarmData )], 100)
  sendEvent(name: "alarmStatus", value: "off");
  */
}

//Toggle LED's
def toggleLED() {
  log.debug("Toggle LED")

  if(device.currentValue("ledStatus") == "auto") {
    ledOn()
  } else if(device.currentValue("ledStatus") == "on") {
    ledOff()
  } else {
    ledAuto()
  }
}

def ledOn() {
/*
  api("decoder_control", "cmd=setInfraLedConfig&mode=1") {}
  */
  hubGet("openInfraLed",[:])
  sendEvent(name: "ledStatus", value: "on");
}

def ledOff() {
/*
  api("decoder_control", "cmd=setInfraLedConfig&mode=1") {}

  */
  hubGet("closeInfraLed",[:])
  sendEvent(name: "ledStatus", value: "off");
}

def ledAuto() {
  hubGet("setInfraLedConfig",[mode:"0"])
  sendEvent(name: "ledStatus", value: "auto");
}

def reboot() {
/*
  api("reboot", "") {
    log.debug("Rebooting")
  }
  */
}

private getHostAddress() {
  return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() )
  return hexport
}
    
def parse(String description) {
  def msg = parseLanMessage(description)
  //log.debug "Received '${state.waitingMethod}' + ${msg.body}"
  
  if (state.waitingMethod == "getMotionDetectConfig") {    
    state.waitingMethod = ""
    def CGI_Result = new XmlSlurper().parseText(msg.body)
    def query = [:]
    
    CGI_Result.children().each { 
    	if (it.name() != "result") {
        	query[it.name()] = it.text();        
        }
    }
	
    state.alarmData = query;
	
  } else if (state.waitingMethod == "setMotionDetectConfig") {
    state.waitingMethod = ""
    def CGI_Result = new XmlSlurper().parseText(msg.body)
    def query = [:]
    
    CGI_Result.children().each { 
    	if (it.name() != "result") {
        	query[it.name()] = it.text();        
        }
    }
    
  } else if (state.waitingMethod == "getDevState" ) {
  	state.waitingMethod = ""
    def CGI_Result = new XmlSlurper().parseText(msg.body)
    def query = [:]
    
    CGI_Result.children().each { 
    
    	if (it.name() == "motionDetectAlarm" &&  it.text() == "0") {
        	sendEvent(name: "alarmStatus", value: "off");
        } else if (it.name() == "motionDetectAlarm" &&  it.text() == "1") {
        	sendEvent(name: "alarmStatus", value: "on");
        } else if (it.name() == "motionDetectAlarm" &&  it.text() == "2") {
        	sendEvent(name: "alarmStatus", value: "alarm");
        }
        
        if (it.name() == "infraLedState" && it.text() == "0" ) {
        	sendEvent(name: "ledStatus", value: "auto")      
        } else  if (it.name() == "infraLedState" && it.text() == "1" ) {
        	sendEvent(name: "ledStatus", value: "manual")      
        }
        
    }
  }  
        
}

private hubGet(def cmd, def parameters) {
	//Setting Network Device Id
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    device.deviceNetworkId = "$iphex:$porthex"
    parameters["usr"] = username;
    parameters["pwd"] = password;
    parameters["cmd"] = cmd;    
    
    state.waitingMethod = cmd;
    
    def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",
        path: "/cgi-bin/CGIProxy.fcgi",
        headers: [HOST:getHostAddress()],
        query: parameters
    )
    
    //log.debug("Executing hubaction " + hubAction )
    
	hubAction
}

def poll() {
  hubGet("getDevState",[:])    
}