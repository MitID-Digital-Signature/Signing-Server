<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xslt="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="xs fn"
                version="3.0">
    <xsl:output indent="yes" omit-xml-declaration="yes"/>
    <xsl:template match="json">
        <xsl:copy-of select="json-to-xml(.)" />
    </xsl:template>
</xsl:stylesheet>
