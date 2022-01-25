/*
Accesses (GET) a webpage, scans response for first occurrence of a string (word/phrase), sets boolean if found
URL must be complete and exact including HTTP:// HTTPS:// (copy from browser address)
Search string must be exact including case, spaces (copy from site’s webpage loaded in browser)
*/

static String version()	{return '0.0.1'}
metadata {
	definition(name: "Webpage search", //Names the driver
	namespace:"me",
	author:"justme")
	{
    capability "Sensor" //Required for device to be in a tile
    capability "Refresh" //Adds "Refresh" to device page, also requires method refresh() or runtime error will occur
    
    //Adds "Current States" to device page. sendevent also required
    attribute "searchstringfound", "bool"
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

    if(response.getStatus() == 200){
        if(response.data.indexOf(searchstring)>=0) searchstringfound=true
        else searchstringfound=false  //Set to false when search string or URL are not found
	}
    else searchstringfound=false //Set to false when search string or URL are not found

    date = new Date()
    
	if(enablelogging){
		log.debug "enablelogging ${enablelogging}"
		log.debug "updatedate ${date.toString()}"
 		log.debug "completeURL ${completeURL}"
		log.debug "searchstring ${searchstring}"
        log.debug "frequency ${frequency}"
        log.debug "searchstringfound ${searchstringfound}"
		log.debug "HTTP status ${response.getStatus()}" //Same as log.debug "HTTP status $response.status"
		log.debug "response.data.length ${response.data.length()}"
        //log.debug "response.data ${response.data}" //Could be lengthy and fill Logs page
	}

    sendEvent(name:"searchstringfound", value:searchstringfound)	
    sendEvent(name:"updatedate", value:date.toString())    
}

//Only a stub method
def initialize(){
    log.debug "***Initialize $device.name***"
}

//Runs one time on installing (creating) device
//Only a stub method
def installed(){
    log.debug "***Installed $device.name***"
}

//Runs on Refresh capability clicked on device page or Preferences updated
def refresh(){
    log.debug "***Refresh $device.name***"
    def params = [uri:"$completeURL", contentType:"text/html", timeout: 15]
    asynchttpGet("asyncHTTPHandler", params)     
}

//Runs one time on uninstalling (removing) device
//Only a stub method
def uninstalled(){
    log.debug "***Uninstalled $device.name***"
}

//Runs on Save Preferences on device page
def updated(){
    log.debug "***Preferences update $device.name***"
    refresh()    
    schedule("15 */$frequency * ? * *", refresh) //Schedule every N minutes 15 seconds into the minute instead of the beginning (00)    
}