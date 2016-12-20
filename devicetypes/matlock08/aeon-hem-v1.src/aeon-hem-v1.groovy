import java.text.SimpleDateFormat

/**
 *  Aeon HEM V1
 *
 *  Copyright 2016 Jose Castellanos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Aeon HEM V1", namespace: "matlock08", author: "Jose Castellanos", oauth: true) {
		capability "Configuration"
		capability "Energy Meter"
		capability "Power Meter"
		capability "Sensor"

		attribute "energyCost", "string"
        
        command "reset"
        command "configure"

		fingerprint deviceId: "0x2101", inClusters: "0x70,0x31,0x72,0x86,0x32,0x80,0x85,0x60"
	}

// simulator metadata
	simulator {
		for (int i = 0; i <= 10000; i += 1000) {
			status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
		}
		for (int i = 0; i <= 100; i += 10) {
			status "energy  ${i} ": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
		}
	}

	// tile definitions
	tiles {
		valueTile("power", "power") {
			state "default", label:'${currentValue} W'
		}
		valueTile("energy", "energy", decoration: "flat") {
			state "default", label:'${currentValue} Kwh'
        }
        valueTile("energyCost", "energyCost", decoration: "flat") {
            state "default", label: '${currentValue}'
		}
		standardTile("reset", "energy", inactiveLabel: false, decoration: "flat") {
			state "default", label:'reset', action:"reset"
		}
		standardTile("refresh", "power", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        standardTile("configure", "command.configure", inactiveLabel: false) {
			state "configure", label:'', action: "configure", icon:"st.secondary.configure"
		}

		main (["power","energy","energyCost"])

		details(["power","energy","energyCost", "reset","refresh", "configure"])
 	}
    
    preferences {
        input "kWhCost", "string", title: "\$/kWh (0.16)", defaultValue: "0.16" as String
    }
}



// ========================================================
// Z-WAVE
// ========================================================

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	
    log.debug "Parse returned ${result?.name} - ${result?.value}"
    
    def date = new Date()
    def sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

    def params = [
        uri: "https://homemonitoring-73711.firebaseio.com/hem.json?auth=9GxXMikppcOtKiTAQRpiQFI0mXfodq5zMHDjekFL",
        body: [
            "event": result?.name ,
             "value": result?.value ,
             "unit": result?.unit,
             "date": sdf.format(date)
        ]
    ]

    try {
        httpPostJson(params)
    } catch (e) {
        log.debug "something went wrong: $e"
    }
    
	
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {

    def dispValue
    def newValue
    
	if (cmd.scale == 0) {	
        newValue = cmd.scaledMeterValue
        
        dispValue = String.format("%5.2f",newValue)
        //sendEvent(name: "energy", value: dispValue as String, unit: "kWh")
        state.energyValue = newValue
        BigDecimal costDecimal = newValue * ( kWhCost as BigDecimal)
        def costDisplay = String.format("%5.2f",costDecimal)
        //sendEvent(name: "energyCost", value: "\$${costDisplay}", unit: "")
        [name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
    }
    else if (cmd.scale == 1) {
        [name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
    }
    else {
        [name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W"]
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def refresh() {
	delayBetween([
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format()
	])
}

def reset() {

	sendEvent(name: "energyCost", value: "Cost\n--", unit: "")
    return [
            zwave.meterV2.meterReset().format(),
            zwave.meterV2.meterGet(scale: 0).format()
   ]
}

def configure() {
	def cmd = delayBetween([
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4).format(),  // combined power in watts
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 30).format(), // every 30s
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8).format(),  // combined energy in kWh
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 30).format(), // every 30s
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),  // no third report
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 0).format()   // every 20s
	])
	log.debug cmd
	return cmd
}
