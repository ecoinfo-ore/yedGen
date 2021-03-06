<h5>yedGen-2.1 - Support yed Graph Editor 3.14.2 +</h5>

 > OBDA file generator using yEd Graph Editor v 3.14.2

| Branch    | build status  |
|-----------|---------------|
| [master](https://github.com/rac021/yedGen/tree/master)  |[![Build Status](https://travis-ci.org/ontop/ontop.svg?branch=master)](https://travis-ci.org/rac021/yedGen)|

- Install procedure :

   - ` mvn clean install assembly:single `

- Arguments :
 
   - `-d   : Directory where graphml files are located `
   - `-out : Path of output mapping obda file `
   - `-ext : extension of files involved in the process `
   - `-csv : Path of the CSV file ( containing variables )`
   - `-ig  : include listed variables in graphml files`
   - `-v   :  enable verbose mode // not implemented yet`   
   

- Example :

```
❯   java -cp target/yedGen_2.0-1.0-SNAPSHOT-jar-with-dependencies.jar entypoint.Main  \
   -d 'src/main/resources/demo'                                                       \
   -out './mapping.obda'                                                              \
   -csv './my_csv.csv'                                                                \
   -ext '.graphml'
```

- Supports features :

   - **FullGraph** : One graph -> One obda file mapping
   - **GraphChunks** : Multiple graphs -> one obda file mapping
   - **PatternGraphChunks** : One [ multiple ] graph(s) -> multiple obda files
 
----------------------------------------------------------------------------------

- Description of [**PatternGraphChunks**]( https://github.com/rac021/yedGen/blob/master/README.md#graphml-files-example-) format :

   *  **1- Description of Patterns declarations :**
   
      `##PATTERN_CODE` : Declare a specific pattern

      `NUM` : Index of the first Entity ( the following entities will have an incremental index )
      
      `URI` : pattern URI for variables. Do not touch the **?VARIABLE**
      
      `ObjectProperty` : used as object property between entities of the pattern
      
      `[ ObjectProperty_Entity SqlQueryIndex ]` : Object Property "_" Entity SqlQueryIndex

   * Example :

      - The following pattern 
   
```
        ##PATTERN_1  20  ola/observation/?ENTITY/{dty_code}/{mesure_id} 
           oboe-core:hasContext 
            [ oboe-core:Observation_Ammonium Q_20 ] 
            [ oboe-core:Observation_Solutes Q_21 ] 
            [ oboe-core:Observation_Water Q_22 ]

```  

 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 *Gives the following mapping :*
         
 
 ```
      - mappingId	(PATTERN_1)_Ammonium_20
      
      - target ola/observation/ammonium/{dty_code}/{mesure_id} a oboe-core:Observation 
        oboe-core:ofEntity :Ammonium 
        oboe-core:hasContext ola/observation/solutes/{dty_code}/{mesure_id}
      
      - source : Query_20
```  

```  
      - mappingId	(PATTERN_1)_Solutes_21
      
      - target ola/observation/solutes/{dty_code}/{mesure_id} a oboe-core:Observation
        oboe-core:ofEntity :Solutes 
        oboe-core:hasContext ola/observation/water/{dty_code}/{mesure_id}
      
      - source : Query_21
   
```

```  
      - mappingId	(PATTERN_1)_Water_22
      
      - target ola/observation/water/{dty_code}/{mesure_id} a oboe-core:Observation 
        oboe-core:ofEntity :Water oboe-core:hasContext ...`
      
      - source : Query_22
   
```  
   
----------------------------------------------------------------------------------------

## Details : 

![pattern_mapping_details](https://cloud.githubusercontent.com/assets/7684497/19231617/5297477c-8edb-11e6-8216-4508e91044d9.png)

----------------------------------------------------------------------------------------

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; *  **2- Description of the Variables declarations :**
   
          ?VARIABLE : Declare Variable
   
          ##PATTERN_CODE : Link current Variable to the Pattern PATTERN_CODE.
        
          :Variable : name of the Entity concerned by this Variable
         
          { ?Matcher=value } : replace all matchers in the result mapping by value ( for each variable )
   

   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; * Example :

   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - The following declaration 

```  
         ?VARIABLE  ##PATTERN_1 :Nitrogen 
         { ?VAR_URI=nitrogen } 
         { ?standardVar=oboe-standard:MilligramPerLiter } 
         { ?NUM_DTYPE=11 } 
         { ?FILTER_VAR='azote ammoniacal'  } 
      
```  


 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; *reliying to the ##PATTERN_1 will Replace :*

   ```  
        ?VARIABLE     in the Graph BY   ":Nitrogen"
        ?VAR_URI                   BY   nitrogen   ( interesting for example for customise sql queries )
        ?standardVar               BY   oboe-standard:MilligramPerLiter
    
``` 

----------------------------------------------------------------------------------


### Graphml files example :
 
   - [Graph](https://github.com/rac021/yedGen/blob/master/src/main/resources/ola_mapping/patternGraphChunks/byCategory/physicochimie/physicochimie_sub_01.graphml)

![graphchunks](https://cloud.githubusercontent.com/assets/7684497/17357917/617c5234-595f-11e6-8b72-5f0ee9615828.jpg)

----------------------------------------------------------------------------------

   - [URIs](https://github.com/rac021/yedGen/blob/master/src/main/resources/ola_mapping/patternGraphChunks/byCategory/physicochimie/physicochimie_sub_01.graphml)

![uris](https://cloud.githubusercontent.com/assets/7684497/17358066/27b5ed2a-5960-11e6-887f-3b2cb5641e4f.jpg)

----------------------------------------------------------------------------------

   - [Queries](https://github.com/rac021/yedGen/blob/master/src/main/resources/ola_mapping/patternGraphChunks/byCategory/physicochimie/physicochimie_sub_01.graphml)

![pattern_queries](https://cloud.githubusercontent.com/assets/7684497/17855399/d4be1c3c-6878-11e6-969a-f98deb9c0005.png)

----------------------------------------------------------------------------------

   - [Patterns + Variables](https://github.com/rac021/yedGen/blob/master/src/main/resources/ola_mapping/patternGraphChunks/byCategory/physicochimie/physicochimie_sub_01.graphml)

![pattern_variables](https://cloud.githubusercontent.com/assets/7684497/19230055/71977920-8ed2-11e6-9e9a-da0405c3e763.png)

----------------------------------------------------------------------------------

   - [Connexion](https://github.com/rac021/yedGen/blob/master/src/main/resources/ola_mapping/patternGraphChunks/connexion/connexion.graphml)
<p align="center">
![connexion](https://cloud.githubusercontent.com/assets/7684497/17358431/4cb8b362-5962-11e6-9dce-3ccb9a59e9c4.jpg)
</p>


----------------------------------------------------------------------------------
----------------------------------------------------------------------------------

## Legends ( AnaEE-France ) :

![legendes](https://cloud.githubusercontent.com/assets/7684497/17859538/4de12508-688a-11e6-9c00-f1d625fa0fe3.png)



