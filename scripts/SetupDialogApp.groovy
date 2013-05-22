import org.codehaus.groovy.grails.scaffolding.*
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.commons.*
import org.open_t.dialog.*

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << grailsScript("_GrailsBootstrap")


target ('default': "Set up a dialog-based grails application") {
	depends(checkVersion, parseArguments)
		
		
	
	// AppNameResources.groovy
	def grailsAppTitle=grailsAppName[0].toUpperCase()+grailsAppName.substring(1)
	generateFile "${basedir}/grails-app/conf/${grailsAppTitle}Resources.groovy",	
"""
modules = {    
    todo {
        dependsOn 'dialog,dialog-altselect,dialog-dataTables,bootstrap-responsive-css,bootstrap-tooltip,bootstrap-popover,bootstrap-modal,dialog-bootstrap,dialog-autocomplete,dialog-last'
    }
}
"""
	// spring/resources.groovy
	generateFile "${basedir}/grails-app/conf/spring/resources.groovy" ,
	"""// Generated by setup-dialog-app\nimport org.open_t.dialog.*;
beans = {
    dialogPropertyEditorRegistrar(DialogPropertyEditorRegistrar.class) {}
    
}
"""
	// views/index.gsp
	
	generateFile "${basedir}/grails-app/views/index.gsp" ,
"""<!doctype html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>New Dialog-based Grails application</title>
	</head>
	<body>
		<div id="page-body" role="main" class="body">
			<h1>New Dialog-based Grails application</h1>
			<p>This is the default landing page of a newly created Dialog-based Grails application.</p>
			<p>This page is in grails-app/views/index.gsp .</p>
		</div>		
	</body>
</html>
"""

// views/layouts/main.gsp

generateFile "${basedir}/grails-app/views/layouts/main.gsp" ,
"""<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
    <head>
        <title><g:layoutTitle default="${grailsAppTitle}" /></title>
        <r:require modules="${grailsAppName}"/>
		<r:layoutResources/>
		<g:layoutHead />
		<dialog:head />
	</head>
    <body>
   		<div class="navbar navbar-inverse navbar-fixed-top">
			<div class="navbar-inner">
        		<div class="container">
          			<g:render template="/layouts/menu" />
				</div>
			</div>
		</div>
        <div class="container" id="page">
        	<div class="row">
        		<div class="span12" style="margin-top:45px;">	        		
					<div class="row"><div class="span12" id="statusmessage"></div></div>
	        		<g:if test="\${flash.message}">
	        	    	<div class="alert alert-success">
	    					<button type="button" class="close" data-dismiss="alert">×</button>
	    					\${flash.message}
	    				</div>
	   				</g:if>
	        		<g:if test="\${flash.errorMessage}">
	        	    	<div class="alert alert-error">
	    					<button type="button" class="close" data-dismiss="alert">×</button>
	    					\${flash.errorMessage}
	    				</div>
	   				</g:if>
	    			<g:layoutBody />
	    			<r:layoutResources />
         		</div>
        	</div>
		</div>
    </body>
</html>
"""


}


def generateFile (path,text) {
	if (new File(path).exists()) {
		if (!confirmInput("${path} already exists. Overwrite?")) {
			return
		}
	}
	new File(path).write(text)
	grailsConsole.addStatus "File ${path} generated."
}
