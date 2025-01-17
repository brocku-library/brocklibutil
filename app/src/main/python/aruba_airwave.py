# This piece of code runs locally as a standalone executable (that has nothing to do with
# the rest of the codebase) in Pranjal's machine, since ITS and us weren't able to
# figure out how to get access to a specific Docker container from ITS's side.
# PyInstaller can be used to make Python scripts executable.

from http.cookiejar import LWPCookieJar
import requests
import urllib3
from datetime import datetime
import xml.etree.ElementTree as ET
import re
import json

urllib3.disable_warnings()

def check_match(location, building, floor):
    regex = re.compile(r"([A-Z]+)([0-9]?[0-9])([0-9][0-9])([A-Z]?)[-](.*)")
    match = regex.match(location)

    if match:
        loc_building = match.group(1)
        loc_floor = match.group(2)

        if (((building == "") or (building == loc_building)) and ((floor == "") or (floor == loc_floor))):
            return True
        
    return False

def get_xml_data() -> dict:
    cookie_file = "/tmp/cookies"
    jar = LWPCookieJar(cookie_file)

    try:
        jar.load()
    except:
        pass

    req = requests.Session()
    req.cookies = jar

    headers = {
        "Content-Type": "application/x-www-form-urlencoded"
    }

    # It's still an old Aruba endpoint, that still uses cookies to give API access :')
    data = {
        "destination": "/",
        "credential_0": "<ARUBA_USERNAME>",
        "credential_1": "<ARUBA_PASSWORD>"
    }

    try:
        response = req.post("https://cits-slairwave.campus.brocku.local/LOGIN", headers=headers, data=data, verify=False, timeout=5)
    except:
        return {}

    token = response.headers['X-BISCOTTI']

    headers = {
        "Content-Type": "application/xml",
        "X-BISCOTTI": token
    }

    try:
        xmlData = req.get("https://cits-slairwave.campus.brocku.local/ap_list.xml", headers=headers, verify=False).text
    except:
        return {}

    root = ET.fromstring(xmlData)
    built_dict = {}

    for child in root:
        try:
            built_dict[child.attrib["id"]] = {
                "count": int(child.find("client_count").text) if child.find("client_count") != None else 0,
                "name": child.find("name").text
            }
            
        except:
            print(child.attrib["id"])

    return built_dict

def fetch(building="", floor=""):    
    return sum([v["count"] for (k, v) in get_xml_data().items() if check_match(v["name"], building, floor)])

def fetch(xmlData: dict, building="", floor=""):
    return sum([v["count"] for (k, v) in xmlData.items() if check_match(v["name"], building, floor)])

def fetchByRoom(xmlData: dict, room:str=""):
    return sum([v["count"] for (k , v) in xmlData.items() if v["name"].startswith(room)])

if __name__ == "__main__":
    xml_data = get_xml_data()
    
    floor_data_wifi = []
    floor_data_patron = []
    time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # This number is basically defines how many devices each individual is using on average. 
    # We figured out this number from a linear regression model, based on the device count and real patron count.
    div_factor = 2.0 if (datetime.now().hour >= 11) else 1.7

    # The following lines explicitly works with library spaces. Might need change if more spaces needs to be covered.
    for floor in range(3, 12):
        floor_str = str(floor)
        count = fetch(xml_data, "ST", str(floor))
        
        dataWiFi = {
            "field_131": floor_str,
            "field_132": count,
            "ts_start": time
        }
        floor_data_wifi.append(dataWiFi)

        dataPatron = {
            "field_138": floor_str,
            "field_139": count // div_factor,
            "ts_start": time
        }
        floor_data_patron.append(dataPatron)

    # add Main floor data
    count = fetchByRoom(xml_data, "ST225")
    
    floor_data_wifi.append({
        "field_131": "2",
        "field_132": count,
        "ts_start": time
    })

    floor_data_patron.append({
        "field_138": "2",
        "field_139": count // div_factor,
        "ts_start": time
    })
    
    # add Makerspace data
    count = fetchByRoom(xml_data, "RFP204")

    floor_data_wifi.append({
        "field_131": "20",
        "field_132": count,
        "ts_start": time
    })

    floor_data_patron.append({
        "field_138": "20",
        "field_139": count // div_factor,
        "ts_start": time
    })
        
    # add MDGL data
    count = fetchByRoom(xml_data, "MCC306-1B")
    
    floor_data_wifi.append({
        "field_131": "30",
        "field_132": count,
        "ts_start": time
    })

    floor_data_patron.append({
        "field_138": "30",
        "field_139": count // div_factor,
        "ts_start": time
    })
    
    # get access token
    token_url = "https://brocku.libinsight.com/v1.0/oauth/token"

    headers = {
        "Content-Type": "application/json",
    }

    body = {
        "client_id": "<LIBINSIGHT_CLIENT_ID>",
        "client_secret": "<LIBINSIGHT_CLIENT_SECRET>",
        "grant_type": "client_credentials"
    }

    token = requests.post(token_url, headers=headers, json=body).json()["access_token"]

    # store data
    store_data_url_wifi = "https://brocku.libinsight.com/post/v1.0/custom/{}/type/1/save".format(12260)
    store_data_url_patron = "https://brocku.libinsight.com/post/v1.0/custom/{}/type/1/save".format(12433)
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + token
    }

    response = requests.post(store_data_url_wifi, headers=headers, json=floor_data_wifi)
    response = requests.post(store_data_url_patron, headers=headers, json=floor_data_patron)

    # Pushing the data to LibUtils backend directly, instead of relying on LibInsight, since LibInsight often responds with stale data.
    response = requests.post("http://rtod.library.brocku.ca:32777/busylib", data= {"jsonString": json.dumps(floor_data_wifi)})
