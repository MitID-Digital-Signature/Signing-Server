<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="fn"
                version="3.0">
    <xsl:output method="text" indent="no"/>
    <xsl:template match="/">
        <xsl:value-of select="//fn:map[@key='text']/fn:string[@key='title']"/><xsl:text>&#10;</xsl:text>
        <xsl:text>&#10;</xsl:text>
        <xsl:for-each select="//fn:map[@key='text']/fn:array[@key='content_html']/fn:array">
            <xsl:value-of select="fn:string"/><xsl:text>&#10;</xsl:text>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
