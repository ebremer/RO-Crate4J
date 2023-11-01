package com.ebremer.rocrate4j;

import com.ebremer.rocrate4j.writers.FolderWriter;
import com.ebremer.rocrate4j.writers.ZipWriter;
import java.io.File;

/**
 *
 * @author erich
 */
public class Test {
    public static void main(String[] args) {
        ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new ZipWriter(new File("d:\\nlms2\\halcyon\\hi.zip")));
        //builder.getRDE().setName("this is a test");
        builder.getManifest()
            .AddCreator("http://orcid.org/0000-0003-0223-1059")
            .AddKeyword("Whole Slide Imaging")
            .AddKeyword("pathology")
            .AddKeyword("nuclear segmentation")
            .AddPublisher("https://ror.org/05qghxh33")
            .AddPublisher("https://ror.org/01882y777");
        builder.build();
        
        builder = new ROCrate.ROCrateBuilder(new FolderWriter(new File("d:\\nlms2\\halcyon\\hidirect")));
        //builder.manifest.setName("this is a test");
        builder.build();
    }
    
}
