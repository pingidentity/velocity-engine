package org.apache.velocity.anakia;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Velocity", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import org.apache.velocity.Template;
import org.apache.velocity.runtime.Runtime;
import org.apache.velocity.util.StringUtils;

import org.apache.velocity.VelocityContext;

/**
 * The purpose of this Ant Task is to allow you to use 
 * Velocity as an XML transformation tool like XSLT is.
 * So, instead of using XSLT, you will be able to use this 
 * class instead to do your transformations. It works very
 * similar in concept to Ant's &lt;style&gt; task.
 * <p>
 * You can find more documentation about this class on the
 * Velocity 
 * <a href="http://jakarta.apache.org/velocity/anakia.html">Website</a>.
 *   
 * @author <a href="jon@latchkey.com">Jon S. Stevens</a>
 * @version $Id: AnakiaTask.java,v 1.14 2001/03/04 21:24:14 jon Exp $
 */
public class AnakiaTask extends MatchingTask
{
    /** Default SAX Driver class to use */
    private static final String DEFAULT_SAX_DRIVER_CLASS =
        "org.apache.xerces.parsers.SAXParser";

    /** <code>{@link SAXBuilder}</code> instance to use */
    private SAXBuilder builder;

    /** the destination directory */
    private File destDir = null;
    
    /** the base directory */
    private File baseDir = null;

    /** the style= attribute */
    private String style = null;
    
    /** the File to the style file */
    private File styleFile = null;
    
    /** last modified of the style sheet */
    private long styleSheetLastModified = 0;

    /** the projectFile= attribute */
    private String projectAttribute = null;
    
    /** the File for the project.xml file */
    private File projectFile = null;
    
    /** last modified of the project file if it exists */
    private long projectFileLastModified = 0;

    /** check the last modified date on files. defaults to true */
    private boolean lastModifiedCheck = true;

    /** the default output extension is .html */
    private String extension = ".html";

    /** the template path */
    private String templatePath = null;

    /** the file to get the velocity properties file */
    private File velocityPropertiesFile = null;

    /**
     * Constructor creates the SAXBuilder.
     */
    public AnakiaTask()
    {
        builder = new SAXBuilder(DEFAULT_SAX_DRIVER_CLASS);
    }

    /**
     * Set the base directory.
     */
    public void setBasedir(File dir)
    {
        baseDir = dir;
    }
    
    /**
     * Set the destination directory into which the VSL result
     * files should be copied to
     * @param dirName the name of the destination directory
     */
    public void setDestdir(File dir)
    {
        destDir = dir;
    }
    
    /**
     * Allow people to set the default output file extension
     */
    public void setExtension(String extension)
    {
        this.extension = extension;
    }
    
    /**
     * Allow people to set the path to the .vsl file
     */
    public void setStyle(String style)
    {
        this.style = style;
    }
    
    /**
     * Allow people to set the path to the project.xml file
     */
    public void setProjectFile(String projectAttribute)
    {
        this.projectAttribute = projectAttribute;
    }

    /**
     * Set the path to the templates.
     * The way it works is this:
     * If you have a Velocity.properties file defined, this method
     * will <strong>override</strong> whatever is set in the 
     * Velocity.properties file. This allows one to not have to define
     * a Velocity.properties file, therefore using Velocity's defaults
     * only.
     */
    public void setTemplatePath(String templatePath)
    {
        this.templatePath = templatePath;
    }
    
    /**
     * Allow people to set the path to the velocity.properties file
     * This file is found relative to the path where the JVM was run.
     * For example, if build.sh was executed in the ./build directory, 
     * then the path would be relative to this directory.
     * This is optional based on the setting of setTemplatePath().
     */
    public void setVelocityPropertiesFile(File velocityPropertiesFile)
    {
        this.velocityPropertiesFile = velocityPropertiesFile;
    }
    
    /**
     * Turn on/off last modified checking. by default, it is on.
     */
    public void setLastModifiedCheck(String lastmod)
    {
        if (lastmod.equalsIgnoreCase("false") || lastmod.equalsIgnoreCase("no") 
                || lastmod.equalsIgnoreCase("off"))
        {
            this.lastModifiedCheck = false;
        }
    }

    /**
     * Main body of the application
     */
    public void execute () throws BuildException
    {
        DirectoryScanner scanner;
        String[]         list;
        String[]         dirs;

        if (baseDir == null)
        {
            baseDir = project.resolveFile(".");
        }
        if (destDir == null )
        {
            String msg = "destdir attribute must be set!";
            throw new BuildException(msg);
        }
        if (style == null) 
        {
            throw new BuildException("style attribute must be set!");
        }

        if (velocityPropertiesFile == null)
        {
            velocityPropertiesFile = new File("velocity.properties");
        }

        // If the props file doesn't exist AND a templatePath hasn't 
        // been defined, then throw the exception. otherwise, make
        // the propertiesFile null here so that we can check for it
        // when we initialize the Runtime.
        if (!velocityPropertiesFile.exists() && templatePath != null)
        {
            velocityPropertiesFile = null;
        }
        else if (templatePath != null)
        {
            throw new BuildException ("Could not locate velocity.properties file: " + 
                velocityPropertiesFile.getAbsolutePath());
        }

        log("Transforming into: " + destDir.getAbsolutePath(), Project.MSG_INFO);

        // projectFile relative to baseDir
        if (projectAttribute != null && projectAttribute.length() > 0)
        {
            projectFile = new File(baseDir, projectAttribute);
            if (projectFile.exists())
                projectFileLastModified = projectFile.lastModified();
            else
            {
                log ("Project file is defined, but could not be located: " + 
                    projectFile.getAbsolutePath(), Project.MSG_INFO );
                projectFile = null;
            }
        }
        
        try
        {
            // initialize Velocity
            if (velocityPropertiesFile == null)
            {
                Runtime.init();
            }
            else
            {
                Runtime.init(velocityPropertiesFile.getAbsolutePath());
            }
            // override the templatePath if it exists
            if (templatePath != null && templatePath.length() > 0)
            {
                Runtime.setSourceProperty(Runtime.FILE_RESOURCE_LOADER_PATH,
                    templatePath);
            }

            // get the last modification of the VSL stylesheet
            styleSheetLastModified = Runtime.getTemplate(style).getLastModified();
        }
        catch (Exception e)
        {
            log("Error: " + e.toString(), Project.MSG_INFO);
            throw new BuildException(e);
        }
        
        // find the files/directories
        scanner = getDirectoryScanner(baseDir);

        // get a list of files to work on
        list = scanner.getIncludedFiles();
        for (int i = 0;i < list.length; ++i)
        {
            process( baseDir, list[i], destDir );
        }
    }    
    
    /**
     * Process an XML file using Velocity
     */
    private void process(File baseDir, String xmlFile, File destDir)
        throws BuildException
    {
        File   outFile=null;
        File   inFile=null;
        Writer writer = null;
        try
        {
            // the current input file relative to the baseDir
            inFile = new File(baseDir,xmlFile);
            // the output file relative to basedir
            outFile = new File(destDir,xmlFile.substring(0,xmlFile.lastIndexOf('.'))+extension);

            // only process files that have changed
            if (lastModifiedCheck == false || (inFile.lastModified() > outFile.lastModified() ||
                    styleSheetLastModified > outFile.lastModified() ||
                    projectFileLastModified > outFile.lastModified()))
            {
                ensureDirectoryFor( outFile );

                //-- command line status
                log("Input:  " + xmlFile, Project.MSG_INFO );
                log("Output: " + outFile, Project.MSG_INFO );
                // Build the JDOM Document
                Document root = builder.build(inFile);
                // Build the Project file document
                // FIXME: this should happen in the execute method since
                // it really only needs to be done once
                Document projectDocument = null;
                if (projectFile != null)
                    projectDocument = builder.build(projectFile);
    
                // Shove things into the Context

                VelocityContext context = new VelocityContext();

                context.put ("root", root.getRootElement());
                context.put ("xmlout", new XMLOutputter());
                context.put ("relativePath", getRelativePath(xmlFile));
                context.put ("treeWalk", new TreeWalker());
                context.put ("xpath", new XPathTool() );
                context.put ("escape", new Escape() );
                context.put ("date", new java.util.Date() );

                // only put this into the context if it exists.
                if (projectDocument != null)
                    context.put ("project", projectDocument.getRootElement());

                // Process the VSL template with the context and write out
                // the result as the outFile.
                writer = new BufferedWriter(new FileWriter(outFile));
                // get the template to process
                Template template = Runtime.getTemplate(style);
                template.merge(context, writer);
            }
        }
        catch (JDOMException e)
        {
            if (e.getRootCause() != null)
            {
                e.getRootCause().printStackTrace();
            }
            else
            {
                e.printStackTrace();
            }
//            log("Failed to process " + inFile, Project.MSG_INFO);
            if (outFile != null ) outFile.delete();
        }
        catch (Throwable e)
        {
//            log("Failed to process " + inFile, Project.MSG_INFO);
            if (outFile != null ) outFile.delete();
            e.printStackTrace();
        }        
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.flush();
                    writer.close();
                }
                catch (Exception e)
                {
                }
            }
        }
    }
    
    /**
     * Hacky method to figure out the relative path
     * that we are currently in. This is good for getting
     * the relative path for images and anchor's.
     */
    private String getRelativePath(String file)
    {
        if (file == null || file.length()==0)
            return "";
        StringTokenizer st = new StringTokenizer(file, "/\\");
        // needs to be -1 cause ST returns 1 even if there are no matches. huh?
        int slashCount = st.countTokens() - 1;
        StringBuffer sb = new StringBuffer();        
        for (int i=0;i<slashCount ;i++ )
        {
            sb.append ("../");
        }
        if (sb.toString().length() > 0)
            return StringUtils.chop(sb.toString(), 1);
        else
            return ".";    
    }
    
    /**
     * create directories as needed
     */
    private void ensureDirectoryFor( File targetFile ) throws BuildException {
        File directory = new File( targetFile.getParent() );
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new BuildException("Unable to create directory: " 
                                         + directory.getAbsolutePath() );
            }
        }
    }
}    
