/**
 *  Aeon Home Energy Meter V1
 *  DSB09104ZWUS
 *  Author: SmartThings
 *  Modified by: PsychoBob
 *  Modified by: Matlock
 *  Date: 2015-07-03
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Aeon Home Energy Meter V1 w/Cost", namespace: "PsychoBob", author: "PsychoBob") {
		capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"

        attribute "energyCost", "string" 

		command "reset"

		fingerprint deviceId: "0x2101", inClusters: " 0x70,0x31,0x72,0x86,0x32,0x80,0x85,0x60"
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

		graphTile(name: "powerGraph", attribute: "power", action:"graph")
        		
		main (["power","energy","energyCost"])

		details(["powerGraph","power","energy","energyCost", "reset","refresh", "configure"])
 	}
    
    preferences {
        input "kWhCost", "string", title: "\$/kWh (0.16)", defaultValue: "0.16" as String
    }
}

// ========================================================
// PREFERENCES
// ========================================================

preferences {
	input name: "graphPrecision", type: "enum", title: "Graph Precision", description: "Daily", required: true, options: graphPrecisionOptions(), defaultValue: "Daily"
	input name: "graphType", type: "enum", title: "Graph Type", description: "line", required: false, options: graphTypeOptions(), defaultValue: "line"
}

// ========================================================
// MAPPINGS
// ========================================================

mappings {
	path("/graph") {
		action:
		[
			GET: "renderGraph"
		]
	}
}

def renderGraph() {
	log.debug "renderGraph"
	def data = fetchGraphData("power")
	def averageData = data*.average

	def xValues = data*.unixTime

	def yValues = [
		Total: [color: "#49a201", data: averageData, type: "line"]
	]
    
    log.debug "yValues ${yValues}"
    log.debug "xValues ${xValues}"

	renderGraph(attribute: "power", xValues: xValues, yValues: yValues, focus: "Total", label: "Watts")
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
	log.debug "Parse returned ${result?.name} - ${result?.descriptionText}"

	storeGraphData(result.name, result.value)

	return result
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {

    def dispValue
    def newValue
    
	if (cmd.scale == 0) {	
        newValue = cmd.scaledMeterValue
        
        dispValue = String.format("%5.2f",newValue)
        sendEvent(name: "energy", value: dispValue as String, unit: "kWh")
        state.energyValue = newValue
        BigDecimal costDecimal = newValue * ( kWhCost as BigDecimal)
        def costDisplay = String.format("%5.2f",costDecimal)
        sendEvent(name: "energyCost", value: "\$${costDisplay}", unit: "")
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
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4).format(),   // combined power in watts
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 20).format(), // every 20s
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8).format(),   // combined energy in kWh
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 20).format(), // every 20s
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),    // no third report
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 20).format() // every 20s
	])
	log.debug cmd
	cmd
}


