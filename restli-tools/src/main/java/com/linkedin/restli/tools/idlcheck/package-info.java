/**
 * idl (.restspec.json) file backwards compatibility checker. Given pairs of idl files, it checks for each pair that
 * if the server behind the current idl can serve all requests from clients which are compliant to the previous idl.
 */
package com.linkedin.restli.tools.idlcheck;