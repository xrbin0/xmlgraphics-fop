/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.visual;

import org.apache.fop.apps.MimeConstants;

/**
 * BitmapProducer implementation that uses the PSRenderer and an external converter 
 * to create bitmaps.
 * <p>
 * See the superclass' javadoc for info on the configuration format.
 */
public class BitmapProducerPS extends AbstractPSPDFBitmapProducer {

    /** @see org.apache.fop.visual.AbstractPSPDFBitmapProducer#getTargetExtension() */
    protected String getTargetExtension() {
        return "ps";
    }
    
    /** @see org.apache.fop.visual.AbstractPSPDFBitmapProducer#getTargetFormat() */
    protected String getTargetFormat() {
        return MimeConstants.MIME_POSTSCRIPT;
    }
    

}
