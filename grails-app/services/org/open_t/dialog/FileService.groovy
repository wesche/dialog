package org.open_t.dialog

import java.text.SimpleDateFormat

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFileFilter

class FileService {

	static transactional = false

	def grailsApplication
    def g = new org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib()

	def uploadFile(request,params,fileCategory="images",dc=null) {
		def filename
		def is
		def mimetype
		if (params.qqfile.class.name=="org.springframework.web.multipart.commons.CommonsMultipartFile") {
			filename=params.qqfile.getOriginalFilename()
			is =params.qqfile.getInputStream()
			mimetype=params.qqfile.getContentType()
		} else {
			filename=params.qqfile
			is =request.getInputStream()
			mimetype=request.getHeader("Content-Type")
		}

		char[] cbuf=new char[100000]
		byte[] bbuf=new byte[100000]

		File tempFile=File.createTempFile("upload", "bin");
		OutputStream os=new FileOutputStream(tempFile)

		int nread =is.read(bbuf, 0, 100000)
		int total=nread
		while (nread>0) {
			os.write(bbuf, 0, nread)
			nread =is.read(bbuf, 0, 100000)
			if (nread>0)
				total+=nread
		}
		os.flush()

		is.close()
		os.close()
		if (params.direct && dc!=null && (params.identifier!=null && params.identifier!="null")) {
			def diPath=filePath(dc,params.identifier,fileCategory)
			def destFile= new File("${diPath}/${filename}")
			FileUtils.copyFile(tempFile,destFile)
			tempFile.delete()
		}

		def res=[path:tempFile.absolutePath,name:tempFile.name,success:true,mimetype:mimetype,identifier:params.identifier,sFileName:params.sFileName]
		return res
	}

	def pack(n) {
		String s= Long.toString(new Long(n),36)
		return String.format("%1\$8s", s).replace(' ', '0')
	}

	def packedPath(n) {
		String s=pack(n)
		return s.substring(0,2)+"/"+s.substring(2,4)+"/"+s.substring(4,6)+"/"+s.substring(6,8)
	}

	// TODO offer possibility to provide alternate location per category.

	def relativePath(dc,id,fileCategory) {
        def name = dc.class==java.lang.String ? dc : dc.getName()		
		name=name.replaceAll (".*\\.", "")

		Boolean flag=dc.methods.collect { method -> method.name }.contains("getFolderPath")

		if (flag) {
			def dcInstance=dc.get(id)
			return dcInstance.getFolderPath(fileCategory)
		} else {
			return "${fileCategory}/${name}/${packedPath(id)}"
		}
	}

	def filePath(dc,id,fileCategory) {
		def basePath=grailsApplication.config.dialog.files.basePath
		def name = dc.class==java.lang.String ? dc : dc.getName()
		name=name.replaceAll (".*\\.", "")
		return "${basePath}/${relativePath(dc,id,fileCategory)}"
	}

	// TODO offer possibility to provide alternate location per category.

	def fileUrl(dc,id,fileCategory) {
		def baseUrl=grailsApplication.config.dialog.files.baseUrl
        def name = dc.class==java.lang.String ? dc : dc.getName()        
		name=name.replaceAll (".*\\.", "")
		return "${baseUrl}/${fileCategory}/${name}/${packedPath(id)}"
	}

	def filelist(dc,params,fileCategory="images",linkType="external",actions=null) {
		def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",new Locale('nl'))
        	log.debug "PARAMS: ${params}"
		def diUrl=fileUrl(dc,params.id,fileCategory)
		log.info "Documents for documentId ${params.id} are in: ${diUrl}"
		def aaData=[:]
		//def baseUrl=request.contextPath
		if(params.id&& params.id!="null") {
			File dir = new File(filePath(dc,params.id,fileCategory))
			aaData=dir.listFiles().collect { file ->
                def downloadLink
                if (linkType=="external") {
                    downloadLink="${diUrl}/${file.name}"
                } else {
                    log.debug "params: ${params}"
                    downloadLink=g.createLink(action:"streamfile",id:params.id,params:[filename:file.name])
                }
                
                if(!actions) {
        			actions= { aParams,aFile -> 
                        def actionsString="""<div class="btn-group">"""
                        def actionsParameter=aParams.actions?:"none"
                        def actionsList=actionsParameter.split(',')
                        // TODO add other actions ('show','edit')
                        if (actionsList.contains("delete")) {
                            actionsString +="""<span class="btn btn-small" onclick="dialog.deleteFile(${aParams.id},'${aParams.controller}','${aFile.name}',null)">&times;</span>""" 
                        }
                        actionsString+="</div>"
                        return actionsString
                    }
        		}

				[0:"""<a href="${downloadLink}">${file.name}</a>""",
				 1:file.length(),
				 2:format.format(file.lastModified()),
                 3: actions(params,file)]				 
			}.sort { file -> file[new Integer(params.iSortCol_0)] }

			if (params.sSortDir_0=="desc") {
				aaData=aaData.reverse()
			}
		}
		def json = [sEcho:params.sEcho,iTotalRecords:aaData.size(),iTotalDisplayRecords:aaData.size(),aaData:aaData]
	}

	def filelistnolink(dc,params,fileCategory="images",linkType="external",actions=null) {
		def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",new Locale('nl'))
	        log.debug "PARAMS: ${params}"
		def diUrl=fileUrl(dc,params.id,fileCategory)
		log.info "Documents for documentId ${params.id} are in: ${diUrl}"

		def aaData=[:]
		//def baseUrl=request.contextPath
		if(params.id&& params.id!="null") {
			File dir = new File(filePath(dc,params.id,fileCategory))
			aaData=dir.listFiles().collect { file ->
		                def downloadLink
		                if (linkType=="external") {
		                    downloadLink="${diUrl}/${file.name}"
		                } else {
		                    log.debug "params: ${params}"
		                    downloadLink=g.createLink(action:"streamfile",id:params.id,params:[filename:file.name])
		                }
			
	                if(!actions) {
				actions= { aParams,aFile ->
		                        def actionsString="""<div class="btn-group">"""
		                        def actionsParameter=aParams.actions?:"none"
		                        def actionsList=actionsParameter.split(',')
		                        // TODO add other actions ('show','edit')
		                        if (actionsList.contains("view")) {
		                            actionsString +="""<span class="btn btn-small" onclick="javascript: viewDocument('${diUrl}/${aFile.name}')">Bekijken</a>"""
		                        }
		                        if (actionsList.contains("delete")) {
		                            actionsString +="""<span class="btn btn-small" onclick="dialog.deleteFile(${aParams.id},'${aParams.controller}','${aFile.name}',null)">&times;</span>""" 
		                        }
		                        actionsString+="</div>"
					return actionsString
				}
			}

				[0:"""${file.name}""",
				 1:file.length(),
				 2:format.format(file.lastModified()),
		                 3: actions(params,file)]
			}.sort { file -> file[new Integer(params.iSortCol_0)] }

			if (params.sSortDir_0=="desc") {
				aaData=aaData.reverse()
			}
		}
		def json = [sEcho:params.sEcho,iTotalRecords:aaData.size(),iTotalDisplayRecords:aaData.size(),aaData:aaData]
	}

	def filemap(dc,params,fileCategory="images") {
		def diUrl=fileUrl(dc,params.id,fileCategory)
		def diPath=filePath(dc,params.id,fileCategory)
		File dir = new File(diPath)

		def map = dir.listFiles().collect { file ->
			[file:file,url:"${diUrl}/${file.name}"]
		}
		return map
	}

	def imagelist(dc,params,fileCategory="images") {
		def diUrl=fileUrl(dc,params.id,fileCategory)
		def diPath=filePath(dc,params.id,fileCategory)
		File dir = new File(diPath)

		String text="var tinyMCEImageList = new Array("
		dir.eachFile { file ->
			text+="""\n["${file.name}", "${diUrl}/${file.name}"],"""
		}
		if (text[text.length()-1]==",") {
			text=text.substring(0,text.length()-1)
		}
		text+=");"
		return text
	}

	def medialist(dc,params,fileCategory="media") {
		def diUrl=fileUrl(dc,params.id,fileCategory)
		def diPath=filePath(dc,params.id,fileCategory)
		File dir = new File(diPath)

		String text="var tinyMCEMediaList = new Array("
		dir.eachFile { file ->
			text+="""\n["${file.name}", "${diUrl}/${file.name}"],"""
		}
		if (text[text.length()-1]==",") {
			text=text.substring(0,text.length()-1)
		}
		text+=");"
		return text
	}

	def submitFile(dc,id,fileupload,fileCategory="images") {

		def fileInfo=fileupload.split("\\|")

		// create folder structure
		def diPath=filePath(dc,id,fileCategory)
		new File(diPath).mkdirs()
		// upload the file
		File file=new File(fileInfo[1])
		if (file.exists()) {
			def destFile= new File("${diPath}/${fileInfo[0]}")
			FileUtils.copyFile(file,destFile)
			file.delete()
		}
	}

	// TODO error handling: return error that dialog can show
	def submitFiles(dc,params,fileCategory="images") {

		if (params.fileupload) {

			if (params.fileupload.class.name=="java.lang.String") {
				submitFile(dc,params.id,params.fileupload,fileCategory)
			} else {
				params.fileupload.each { fileupload ->
					submitFile(dc,params.id,fileupload,fileCategory)
				}
			}
		}
	}

	def deleteFile(dc,params,fileCategory="images") {
		File file = new File(filePath(dc,params.id,fileCategory)+"/"+params.filename);
		Boolean success= file.delete()
		def result=[success:success,mesage:"${params.filename} deleted"]
		return [result:result]
	}
    
    /**
	 * Stream file
	 * 
	 * @param contentstream The content stream
	 * @param The servlet response to use
	 */
	
	def streamFile(dc,id,fileCategory,name,response) {
        
        def filePath=filePath(dc,id,fileCategory)+"/"+name
        def file=new File(filePath)
        
        response.setHeader("Content-disposition", "attachment; filename=\"" +file.name+"\"")		
        // TODO add Tika file type recognition
		response.setHeader("Content-Type", "application/octet-stream")
		
		def inputStream=new FileInputStream(file)
		def bufsize=100000
		byte[] bytes=new byte[(int)bufsize]

		def offset=0
		def len=1
		while (len>0) {
			len=inputStream.read(bytes, 0, bufsize)
			if (len>0)
			response.outputStream.write(bytes,0,len)
			offset+=bufsize
		}
		response.outputStream.flush()
	}

	/**
	* Copy files from one domain object to another
	*/
	def copyFiles(dc=null, fileCategory="images", fromId, toId) {
	    if( (fromId != null) && (toId != null) ) {
	        File fromDir = new File(filePath(dc,fromId,fileCategory))
                File toDir = new File(filePath(dc,toId,fileCategory))
                FileUtils.copyDirectory(fromDir,toDir,FileFileFilter.FILE)
		log.info "Copied files from directory ${fromDir} to directory ${toDir}"
	    }
	}    
}
