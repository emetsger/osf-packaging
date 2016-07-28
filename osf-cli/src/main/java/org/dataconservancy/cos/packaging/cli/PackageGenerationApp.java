/*
 * Copyright 2016 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.cos.packaging.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.dataconservancy.cos.osf.client.model.Registration;
import org.dataconservancy.cos.osf.client.model.User;
import org.dataconservancy.cos.osf.client.service.OsfService;
import org.dataconservancy.cos.osf.packaging.OsfPackageGraph;
import org.dataconservancy.packaging.tool.api.Package;
import org.dataconservancy.cos.packaging.IpmPackager;
import org.dataconservancy.packaging.tool.model.GeneralParameterNames;
import org.dataconservancy.packaging.tool.model.PackageGenerationParameters;
import org.dataconservancy.packaging.tool.model.PropertiesConfigurationParametersBuilder;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application for generating packages from package descriptions.
 * <p>
 * Required arguments are the URL to the OSF registration,
 *  the path to the  OSF java client configuration file,
 *  the output file/directory that will contain the generated package,
 *  and the required BagIt metadata: the bag name on the command line, and the other metadata
 *      in a properties file
 * </p>
 *
 *
 */
public class PackageGenerationApp {

    @Argument(multiValued = false, usage="URL to the registration to be packaged" )
    private String registrationUrl = null;

     /** Request for help/usage documentation */
    @Option(name = "-h", aliases = {"-help", "--help"}, usage = "print help message")
    private boolean help = false;

    /** the path to the OSF Java client configuration */
    @Option(name = "-c", aliases = {"-configuration", "--configuration"}, required = true, usage = "path to the OSF Java client configuration")
    private static File confFile;

    /** the output directory for the package */
    @Option(name = "-o", aliases = {"-output", "--output"}, usage = "path to the directory where the package will be written")
    private static File outputLocation;

    /** the bag name (required by the BagIt specification) **/
    @Option(name = "-n", aliases = {"-name", "--name"}, required = true, usage = "the name for the bag")
    private static String bagName;

   /** other bag metadata properties file location */
    @Option(name = "-m", aliases = {"-metadata", "--metadata"}, usage = "the path to the metadata properties file for additional bag metadata")
    private static File bagMetadataFile;

    /** Requests the current version number of the cli application. */
	@Option(name = "-v", aliases = { "-version", "--version" }, usage = "print version information")
	private boolean version = false;


    public static void main(String[] args) {

        final PackageGenerationApp application = new PackageGenerationApp();

		CmdLineParser parser = new CmdLineParser(application);
		parser.setUsageWidth(80);

		try {
			parser.parseArgument(args);

			/* Handle general options such as help, version */
			if (application.help) {
				parser.printUsage(System.err);
				System.err.println();
				System.exit(0);
			} else if (application.version) {
				System.err.println(PackageGenerationApp.class.getPackage()
						.getImplementationVersion());
				System.exit(0);
			}


			Properties props = System.getProperties();

            if(confFile.exists() && confFile.isFile()) {
                props.setProperty("osf.client.conf", confFile.toURI().toString());
            } else {
                System.err.println("Supplied OSF Client Configuration File " + confFile.getCanonicalPath() + " does not exist or is not a file.");
                System.exit(1);
            }

            if(!outputLocation.exists() || !outputLocation.isDirectory()){
                System.err.println("Supplied output file directory " + outputLocation.getCanonicalPath() + " does not exist or is not a directory.");
                System.exit(1);
            }

            if(!bagMetadataFile.exists() || !bagMetadataFile.isFile()){
                System.err.println("Supplied bag metadata file " + bagMetadataFile.getCanonicalPath() + " does not exist or is not a file.");
                System.exit(1);
            }

			/* Run the package generation application proper */
			application.run();

		} catch (CmdLineException e) {
			/*
			 * This is an error in command line args, just print out usage data
			 * and description of the error.
			 */
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			System.err.println();
			System.exit(1);
		} catch (Exception e){
            System.err.println(e.getMessage());
           // System.exit(e.getCode());
        }
    }

   	private void run() throws Exception {
        final ClassPathXmlApplicationContext cxt =
                new ClassPathXmlApplicationContext("classpath*:applicationContext.xml",
                    "classpath*:org/dataconservancy/config/applicationContext.xml",
                    "classpath*:org/dataconservancy/packaging/tool/ser/config/applicationContext.xml",
                    "classpath*:org/dataconservancy/cos/osf/client/config/applicationContext.xml",
                    "classpath:/org/dataconservancy/cos/packaging/config/applicationContext.xml");

        final OsfPackageGraph packageGraph = cxt.getBean("packageGraph", OsfPackageGraph.class);
        final OsfService osfService = cxt.getBean("osfService", OsfService.class);
        IpmPackager ipmPackager = new IpmPackager();

        final Registration registration = osfService.registrationByUrl(registrationUrl).execute().body();
        final List<User> users = registration.getContributors().stream()
                .map(c -> {
                    try {
                        if (c.getUserRel() != null) {
                            return osfService.userByUrl(c.getUserRel()).execute().body();
                        } else {
                            String contributorId = c.getId();
                            if (contributorId.contains("-")) {
                                contributorId = contributorId.split("-")[1];
                            }
                            return osfService.user(contributorId).execute().body();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                })
                .collect(Collectors.toList());

        packageGraph.add(registration);
        users.forEach(packageGraph::add);

        PackageGenerationParameters packageGenerationParameters =
                new PropertiesConfigurationParametersBuilder()
                        .buildParameters(new FileInputStream(bagMetadataFile));
        packageGenerationParameters.addParam(GeneralParameterNames.PACKAGE_LOCATION,
                System.getProperty("java.io.tmpdir"));
        packageGenerationParameters.addParam(GeneralParameterNames.PACKAGE_NAME, bagName);

        Package pkg = ipmPackager.buildPackage(packageGraph, packageGenerationParameters);

        /* Now just write the package out to a file in the output location*/
        File packageFile = new File(outputLocation.getAbsolutePath(), bagName + ".tar.gz");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(packageFile);
            IOUtils.copy(pkg.serialize(), out);
            out.close();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        pkg.cleanupPackage();

    }



}

