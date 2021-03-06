
/*
Accesses (GET) a webpage, scans response for first occurrence of a string (word/phrase), sets boolean and string if found.

The URL must be complete and exact including HTTP:// HTTPS:// (copy from browser address).
The search string must be unique and exact including case, spaces, special characters, etc. (copy from site’s webpage loaded in browser).
Content created by webpage dynamic scripts will probably not be found by the driver.
Create a virtual device from the driver to use in a tile.
Rule Manager doesn’t allow the use to Boolean variables; use searchstringfoundstr with strict values of “true” or “false”
*/

public String version()	{return '0.0.2'}

metadata {
	definition(name: "Webpage search", //Names the driver
	namespace:"Five-Nines", //Five-Nines on github, FiveNines on Hubitat forum
	author:"Five-Nines",
	importUrl:"https://github.com/Five-Nines/Hubitat/blob/main/web%20search/webpage%20search.java")
	{
	capability "Sensor" //Required for device to be in a tile
	capability "Refresh" //Adds "Refresh" to device page, also requires method refresh() or runtime error will occur
    
	//Adds "Current States" to device page. sendevent also required
	attribute "searchstringfound", "bool"
	attribute "searchstringfoundstr", "string"
	attribute "updatedate", "string"        
	}
}

//Adds "Preferences" to device page
preferences {
	input name:"completeURL", type:"string", title:"Complete URL", required:true //"URL" is a reserved word in Java?????
	input name:"searchstring", type:"string", title:"Search string", required:true
	input name:"frequency", type:"number", title:"Schedule frequency in minutes (1-59)", required:true, defaultValue:30, range:1..59
	input name:"enablelogging", type:"bool", title:"Enable logging of variables?", defaultValue:false
}

def asyncHTTPHandler(response, data) {

	if(response.getStatus() == 200)
		if(response.data.indexOf(searchstring)!=-1) searchstringfound=true
		else searchstringfound=false  //Set to false when search string or URL are not found
	else searchstringfound=false //Set to false when search string or URL are not found
    
	if(searchstringfound) searchstringfoundstr="true" //Use in Rule Manager
	else searchstringfoundstr="false"
    
	date = new Date()
    
	if(enablelogging){
		settings.each {name, value -> log.debug "${name}: ${value}"} //For input variables
		log.debug "frequency: ${frequency}"
		log.debug "searchstringfound: ${searchstringfound}"
		log.debug "searchstringfoundstr: ${searchstringfoundstr}"        
		log.debug "HTTP status: ${response.getStatus()}" //Same as log.info "HTTP status $response.status"
		log.debug "HTTP error: ${response.hasError()}"
		if(!response.hasError()){ //Catches bad URL
			log.debug "response.data.length: ${response.data.length()}"
			//log.debug "response.data ${response.data}" //Could be lengthy and fill Logs page
		}
	}

	sendEvent(name:"searchstringfound", value:searchstringfound)
	sendEvent(name:"searchstringfoundstr", value:searchstringfoundstr)
	sendEvent(name:"updatedate", value:date.toString())    
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
	def params = [uri:"$completeURL", contentType:"text/html", timeout: 15]
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
	schedule("15 */${frequency} * ? * *", refresh) //Schedule every N minutes 15 seconds into the minute instead of the beginning (00)    
}
