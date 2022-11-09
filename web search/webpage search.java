/*
Accesses (GET) a webpage, scans response for first occurrence of a string (word/phrase), sets boolean and string if found.

The URL must be complete and exact including HTTP:// HTTPS:// (copy from browser address).
The search string must be unique and exact including case, spaces, special characters, etc. (copy from site’s webpage loaded in browser).
Content created by webpage dynamic scripts will probably not be found by the driver.
Create a virtual device from the driver to use in a tile.
Rule Manager doesn’t allow the use of Boolean variables; use searchstringfoundstr with strict values of “true” or “false”

v003:
Add fulltext
v004:
Add Presence Sensor. See for pattern: https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/virtualOmniSensor.groovy
Change schedule seconds to 59 to not interfere with jobs running at 00 seconds
v005:
Rearrange debug output
Valid HTTP response range check of 200-299
Add timeout - added to vary time for server to respond
*/

public String version()	{return '0.0.5'}

metadata {
	definition(name: "Webpage search", //Names the driver
	namespace:"Five-Nines", //Five-Nines on github, FiveNines on Hubitat forum
	author:"Five-Nines",
	importUrl:"https://github.com/Five-Nines/Hubitat/blob/main/web%20search/webpage%20search.java")
	{
	capability "Sensor" //Required for device to be in a tile
	capability "Presence Sensor" //Adds presence=true, notpresent=false
	capability "Refresh" //Adds "Refresh" to device page, also requires method refresh() or runtime error will occur
	command "present"
	command "notpresent"        
    
	//Adds "Current States" to device page. sendevent also required
    attribute "searchstring", "string"
	attribute "searchstringfound", "bool"
	attribute "searchstringfoundstr", "string"
	attribute "updatedate", "string"
    attribute "fulltext", "string"   
	}
}

//Adds "Preferences" to device page
preferences {
	input name:"completeURL", type:"string", title:"Complete URL", required:true //"URL" is a reserved word in Java?????
	input name:"searchstring", type:"string", title:"Search string", required:true
	input name:"frequency", type:"number", title:"Schedule frequency in minutes (1-59) starting at top of the hour", required:true, defaultValue:30, range:1..59
	input name:"timeout", type:"number", title:"Request timeout in seconds (1-300)", required:true, defaultValue:30, range:1..300	
	input name:"enablelogging", type:"bool", title:"Enable logging of variables?", defaultValue:false
}

def asyncHTTPHandler(response, data) {

	if(response.getStatus()>=200 && response.getStatus()<=299)
		if(response.data.indexOf(searchstring)!=-1) searchstringfound=true
		else searchstringfound=false  //Set to false when search string or URL are not found
	else searchstringfound=false //Set to false when search string or URL are not found
    
	if(searchstringfound) searchstringfoundstr="true" //Use in Rule Manager
	else searchstringfoundstr="false"
    
	if(searchstringfound) present() //Use in Notifications
	else notpresent()
    
	date = new Date()
    
    String fulltext=searchstring+"<br>"+searchstringfoundstr+"<br>"+date.toString()
    
	if(enablelogging){
		settings.each {name, value -> log.debug "${name}: ${value}"} //For input variables
		log.debug "HTTP status: ${response.getStatus()}" //Same as log.info "HTTP status $response.status"
		log.debug "HTTP error: ${response.hasError()}"
		log.debug "searchstringfound: ${searchstringfound}"
		log.debug "searchstringfoundstr: ${searchstringfoundstr}"        
        log.debug "fulltext: ${fulltext}"    
		if(!response.hasError()){ //Catches bad URL
			log.debug "response.data.length: ${response.data.length()}"
			//log.debug "response.data ${response.data}" //Could be lengthy and fill Logs page
		}
	}
	
	sendEvent(name:"searchstring", value:searchstring)
	sendEvent(name:"searchstringfound", value:searchstringfound)
	sendEvent(name:"searchstringfoundstr", value:searchstringfoundstr)
	sendEvent(name:"updatedate", value:date.toString())
    sendEvent(name:"fulltext", value:fulltext)
}

//Only a stub method
def initialize(){
	log.info "*** Initialize ${device.name}"
}

//Runs one time on installing (creating) device
//Only a stub method
def installed(){
	log.info "*** Installed ${device.name} version ${version()}"	
}

//Runs on Refresh capability clicked on device page or Preferences updated
def refresh(){
	log.info "*** Refresh ${device.name}"
	def params = [uri:"${completeURL}", contentType:"text/html", timeout:"${timeout}"]
	asynchttpGet("asyncHTTPHandler", params)     
}

//Runs one time on uninstalling (removing) device
//Only a stub method
def uninstalled(){
	log.info "*** Uninstalled ${device.name}"
}

//Runs on Save Preferences on device page
def updated(){
	log.info "*** Preferences update ${device.name}"
	refresh()
	//Schedule every N minutes starting at top of the hour, 59 seconds into the minute instead of minute beginning (00 seconds).
	//This should reduce the load scheduled services place on the beginning of the hour.
	//30 minutes will run 1:00:59, 1:30:59, 2:00:59; 59 minutes will run 1:00:59, 1:59:59, 2:00:59.
	schedule("59 */${frequency} * ? * *", refresh)
}

def present() {
	sendEvent(name: "presence", value: "present", descriptionText: "${device.displayName} present")
}

def notpresent() {
	sendEvent(name: "presence", value: "not present", descriptionText: "${device.displayName} not present")
}
