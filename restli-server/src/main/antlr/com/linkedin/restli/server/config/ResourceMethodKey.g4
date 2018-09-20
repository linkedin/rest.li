grammar ResourceMethodKey;

@header {
  package com.linkedin.restli.server.config;
}

key             : ( restResource | '*' ) '.' ( operation | '*' );
restResource    : Name ( '-' Name )* ( ':' Name )*;
operation       : simpleOp | complex;
simpleOp        : 'GET' | 'BATCH_GET' | 'CREATE' | 'BATCH_CREATE' |
                  'PARTIAL_UPDATE' | 'UPDATE' | 'BATCH_UPDATE' |
                  'DELETE' | 'BATCH_PARTIAL_UPDATE' | 'BATCH_DELETE' |
                  'GET_ALL' | 'OPTIONS';
complex         : complexOp '-' ( Name | '*' );
complexOp       : 'FINDER' | 'ACTION' | 'BATCH_FINDER';
Name            : [a-zA-Z_0-9]+;