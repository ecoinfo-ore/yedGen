
package org.inra.yedgen.processor ;

import java.io.File ;
import java.util.Set ;
import java.util.List ;
import java.io.IOException ;
import java.nio.file.Files ;
import java.nio.file.Paths ;
import java.util.ArrayList ;
import java.util.Map;
import java.util.stream.Stream ;
import java.util.logging.Level ;
import java.util.logging.Logger ;
import java.util.function.Consumer ;
import org.inra.yedgen.graph.utils.Utils ;
import org.inra.yedgen.processor.io.Writer ;
import org.inra.yedgen.graph.entities.Edge ;
import org.inra.yedgen.processor.io.ObdaHeader ;
import org.inra.yedgen.processor.entities.Node ;
import org.inra.yedgen.properties.CsvProperties ;
import org.inra.yedgen.processor.output.Messages ;
import org.inra.yedgen.properties.ObdaProperties ;
import org.inra.yedgen.graph.managers.ManagerEdge ;
import org.inra.yedgen.processor.entities.Variable ;
import org.inra.yedgen.processor.managers.ManagerUri ;
import org.inra.yedgen.graph.managers.ManagerConcept ;
import org.inra.yedgen.graph.managers.GraphExtractor ;
import org.inra.yedgen.processor.managers.ManagerNode ;
import org.inra.yedgen.processor.factories.FactoryNode ;
import org.inra.yedgen.processor.managers.ManagerQuery ;
import org.inra.yedgen.processor.managers.ManagerVariable ;
import org.inra.yedgen.processor.managers.MetaPatternManager ;
import org.inra.yedgen.processor.managers.ManagerPatternContext ;
import org.inra.yedgen.processor.managers.ManagerPatternParallel ;
import static org.inra.yedgen.graph.managers.GraphExtractor.PREFIX_PREDICAT ;

/**
 *
 * @author ryahiaoui 26-10-2016 17:20
 */
public class Processor {
    
    private final ManagerPatternParallel managerPatternParallel  ;
    private final ManagerPatternContext  managerPatternContext   ;
    private final MetaPatternManager     metaPatternManager      ;
    private final ManagerVariable        managerVariable         ;
    private final GraphExtractor         graphExtractor          ;
    private final ManagerNode            managerNode             ;
    private final ObdaHeader             obdaHeader              ;
    
    private final CsvProperties          csvProperties           ;
    
    private boolean                      verbose                 ;
    
    
    public Processor( String  directory     , 
                      String  extensionFile ,
                      String  propertieFile ,
                      String  jsFile        ,
                      String  connecFile    ,
                      String  prefixFile    ,
                      String default_prefix ) throws Exception  {
   
      this.graphExtractor  =  new GraphExtractor ()             ;
        
      graphExtractor.genGraphPopulatingManagers( directory , extensionFile )    ;
          
      if(default_prefix != null ) GraphExtractor.PREFIX_PREDICAT = default_prefix ;
     
      /* Add External Prefixs if prefixFile not null */ 
      updateConnection( connecFile, this.graphExtractor.getSourceDeclaration()) ;
              
      /* Add External Prefixs if prefixFile not null */ 
      updatePrefixs(prefixFile, this.graphExtractor.getPrefixs()) ;

      ManagerUri     managerUri     =  new ManagerUri( graphExtractor.getMapUris())          ;
      ManagerEdge    managerEdge    =  new ManagerEdge( graphExtractor.getMapEdges() )       ;
      ManagerQuery   managerQuery   =  new ManagerQuery( graphExtractor.getMapQueries() )    ;
      ManagerConcept managerConcept =  new ManagerConcept( graphExtractor.getMapConcepts() ) ;
      
      
      FactoryNode factoryNode     = new FactoryNode( managerEdge      ,
                                                     managerConcept   , 
                                                     managerUri       , 
                                                     managerQuery   ) ;
            
      this.managerNode            = instantiateManagerNode ( managerEdge, factoryNode )  ;
        
      this.obdaHeader             = new ObdaHeader(graphExtractor)                       ;
      
      this.managerPatternContext  = new ManagerPatternContext ( graphExtractor.getMapPatternContexts() , 
                                                                managerQuery     , 
                                                                factoryNode  )   ;
      
      this.csvProperties          = new CsvProperties( propertieFile, jsFile )   ;
        
      this.metaPatternManager     = new  MetaPatternManager( graphExtractor.getMetaPatternHash()     ,
                                                             graphExtractor.getMetaPatternVariable() , 
                                                             graphExtractor.getMetaPatternContext()  , 
                                                             graphExtractor.getMetaPatternParallel() ,
                                                             csvProperties                         ) ;
      
      this.managerPatternParallel = new ManagerPatternParallel ( graphExtractor.getMapPatternParallels() , 
                                                                 managerUri     , 
                                                                 factoryNode    ,
                                                                 metaPatternManager ) ;
      
      this.managerVariable        = new ManagerVariable( graphExtractor.getMapVariables() ,
                                                         managerNode                      , 
                                                         managerPatternContext            , 
                                                         managerPatternParallel)          ;
    }
    
    private ManagerNode instantiateManagerNode ( ManagerEdge managerEdge , FactoryNode factoryNode )  {
        
         ManagerNode localManager = new ManagerNode() ;
           
        for ( Edge edge : Utils.getAll(managerEdge.getEdges()) ) {
            
            int    hash      = edge.getHash()      ;
            String subjectId = edge.getSujetId()   ;
            String objectId  = edge.getObjetId()   ;
            String predicat  = edge.getPredicat()  ;
            
            Node node = factoryNode.createNode( hash      , 
                                                subjectId , 
                                                predicat  , 
                                                objectId  , 
                                                PREFIX_PREDICAT ) ; 
            
            Utils.putInMap(localManager.getNodes(), hash, subjectId, node) ;
        }
        
        localManager.cloneNodes() ;
        return  localManager      ;
    
    }
        
    public boolean processFull ( String outputFile , 
                                 String csvFile    , 
                                 String classe     ,
                                 int column        )  {
        
        boolean processCSV       = processOnlyCSV( outputFile, csvFile, classe, column ) ;
        
        boolean processVariables = processOnlyGraphVariables( outputFile ) ;
        
        return processVariables && processCSV ;
      
    }

    public boolean processOnlyGraphVariables ( String outputFile )   {
      
      Messages.printMessageStartProcessVariableGraphGeneration()     ;
      
      for( Variable variable : managerVariable.getVariables()) {
     
         Set<Node> graph      = managerVariable.process( variable )  ;
          
         List<String> outPut  = new ArrayList<>()                    ;
                      
         // Prepare Output Header 
         outPut.addAll(obdaHeader.getHeaderOut())                    ; 
          
         graph.stream().forEach( node -> { outPut.add( node.outputObda())            ; 
                                           checkMatchers( variable.getVariableName() , 
                                                          node.outputObda() ) ; } )  ;
          
         outPut.add(ObdaProperties.MAPPING_COLLECTION_END) ;
                
         try {
              
            String _fileName = outputFile.substring( 0, outputFile.lastIndexOf('.')) ;
            String extension = outputFile.substring( outputFile.lastIndexOf('.'))    ;
             
            String outFile = _fileName + "_Graph_"                             + 
                             variable.getVariableName().replaceFirst(":", "")  + 
                             extension                                         ;
              
            Writer.checkFile( outFile )           ;
            Writer.writeTextFile(outPut, outFile) ;
            
            Messages.printMessageInfoGeneratedVariable( variable.getVariableName()   ,
                                                        outFile                    ) ;

         } catch (IOException ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex) ;
          }
      }
      
      return managerVariable.getVariables().size() > 0  ;
      
   }
    
   public boolean processOnlyCSV ( String outputFile , 
                                   String csvFile    , 
                                   String classe     ,
                                   int column  )     {
         
     Messages.printMessageStartProcessCsvVariableGeneration( csvFile ) ;
     
     if( ! Writer.existFile ( csvFile ) )  {
        Messages.printMessageErrorCSV ( csvFile) ;
        return true ;
     }
      
     /* Check and skip if patterns are null */ 
     
     if( metaPatternManager.getMetaPatternContext()  == null  ||
         metaPatternManager.getMetaPatternVariable() == null  ||
         metaPatternManager.getMetaPatternParallel() == null   )  {
                     
           Messages.printMessageMetaPatternsNull() ;
           return false                            ;
     }
  
     try {
         
         try ( Stream<String> lines = Files.lines(Paths.get(csvFile)).skip(1)) {
             
             lines.forEach (new Consumer<String>() {
                 
                int counter = 0 ;
                
                @Override
                public void accept(String line) {
                    
                 if( classe != null ) {
                      
                   if( column < 0 ) throw new IllegalArgumentException(" Column Num can't be negative ") ;
                    
                   if( line.split(metaPatternManager.getCSV_SEPARATOR()).length < column + 1 ) {
                       System.out.println(" + CSV Column size = " + 
                                              line.split(metaPatternManager.getCSV_SEPARATOR()).length)  ;
                       throw new IllegalArgumentException(" Column [ " + column + " ] Does't exists ! ") ;
                   }
                    
                   if( ! line.split(metaPatternManager.getCSV_SEPARATOR())[column].trim()
                                                      .replaceAll(" +", " ").equals(classe.trim())) {
                      counter ++ ;
                      return     ;
                   }
                 }
                    
                 try {
                                  
                      List<String> outPut    = new ArrayList<>() ;

                      // Prepare Output Header 
                      outPut.addAll(obdaHeader.getHeaderOut())   ; 

                      // Treat Variable
                      // String nLine = csvProperties.process( line )                            ;
                      String patternContext   = metaPatternManager.generatePatternContext(line)  ;
                      String patternVariable  = metaPatternManager.generatePatternVariable(line) ;

                      Variable variable       = managerVariable.transformToVariable( patternVariable    ,
                                                                                      patternContext  ) ;

                      Set<Node> nodes         = managerVariable.process( variable )    ;

                      nodes.stream().forEach( node -> { outPut.add( node.outputObda())            ; 
                                                        checkMatchers( variable.getVariableName() , 
                                                                        node.outputObda() ) ; })  ;

                      outPut.add( ObdaProperties.MAPPING_COLLECTION_END ) ;

                      try {
                         
                         String folder    = Writer.getFolder ( outputFile )   ;
                         String fileName  = Writer.getfileName ( outputFile ) ;
                         String fileNameWithoutExtension = Writer.getFileWithoutExtension(fileName ) ;
                         String extension = Writer.getFileExtension(fileName) ; 
                         
                         String outFile = folder    + File.separator     + 
                                          counter++ + "_" +
                                          fileNameWithoutExtension       +
                                          "_CSV_"                        + 
                                          variable.getVariableName()
                                                  .replaceFirst(":", "") + 
                                          extension                      ;

                         Writer.checkFile( outFile )           ;
                         Writer.writeTextFile(outPut, outFile) ;

                         Messages.printMessageInfoGeneratedVariable( variable.getVariableName() ,
                                                                     outFile                  ) ;

                       } catch (IOException ex) {
                            Logger.getLogger(Processor.class.getName())
                                  .log(Level.SEVERE, null, ex)        ;
                       }
                                               
                    } catch (Exception e) {
                         Logger.getLogger(Processor.class.getName())
                               .log(Level.SEVERE, null, e)         ;
                    }
                }
            }) ;
           
        }
            
      } catch (IOException ex) {
          Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex)  ;
     }
        
     try {
          return Files.lines( Paths.get(csvFile) ).skip(1).count() > 0             ;
     } catch ( IOException ex ) {
          Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex)  ;
      }
         
     return false ;
        
   }

    
    public boolean processOnlyGraphWithoutVariables ( String outputFile ) {
      
         Set<Node> all = managerNode.getAll()                             ;
      
         List<String> outPut    = new ArrayList<>()                       ;
                      
         // Prepare Output Header 
         outPut.addAll(obdaHeader.getHeaderOut())                         ; 
                      
         all.stream().forEach( node -> outPut.add( node.outputObda()) )   ;
         
         outPut.add( ObdaProperties.MAPPING_COLLECTION_END )              ;
                
         try {
              
            String _fileName = outputFile.substring(0, outputFile.lastIndexOf('.')) ;
            String extension = outputFile.substring(outputFile.lastIndexOf('.'))    ;
             
            String outFile   = _fileName + "_Graph_"       + 
                               System.currentTimeMillis()  +
                               extension                   ;
              
            Writer.checkFile( outFile )           ;
            Writer.writeTextFile(outPut, outFile) ;
            
            Messages.printMessageInfoGeneratedVariable( "Undefined Variable" ,
                                                         outFile           ) ;

         } catch (IOException ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex) ;
          }
     
      return all.size() > 0 ;
      
    }
    
    public void process ( String  outputFile              , 
                          String  csvFile                 , 
                          boolean includingGraphVariables ,
                          String classe                   ,
                          int column                      ,
                          boolean verbose )               {
      
        this.verbose = verbose  ;
        
        boolean process = false ; 
        
        if( includingGraphVariables && csvFile != null )                    {
            process = processFull( outputFile, csvFile, classe, column )    ;
        }
        else if ( ! includingGraphVariables && 
                    csvFile != null )                                       {
            process = processOnlyCSV( outputFile, csvFile, classe, column ) ;
        }
        else  {
            process = processOnlyGraphVariables( outputFile )               ;
        }
        
        if( ! process )                                                     {
            processOnlyGraphWithoutVariables( outputFile )                  ;
        }
    }
    
    private void checkMatchers( String variableName , String outLine ) {
   
        String[] patts = outLine.split(" ") ; 
        
        for ( String patt : patts ) {
           if ( patt.contains("?") )
           Messages.printErrorMatcher( variableName , patt ) ; 
        }
    }

    /* Override Prefixs */
    private void updatePrefixs(String prefixFile, Map<String, String> prefixMap ) {
       
        if( prefixFile == null || prefixMap == null ) return ;
         
        try ( Stream<String> lines = Files.lines(Paths.get(prefixFile))) {
            
            lines.forEach ( line  -> {

                          String[] splitedLines = line.replaceAll(" +", " ").trim().split(" ") ;
                          if( splitedLines.length >= 3 ) 
                          prefixMap.put(splitedLines[1].trim(), splitedLines[2].trim().replace("<", "")
                                                                               .replace(">","")) ;
            }) ;
                        
	} catch (IOException e) {
		e.printStackTrace();
	}
    }

    /* Override Coonection Info */ 
    private void updateConnection(String connecFile, Map<String, String> sourceDeclarationMap) {
        
        if( connecFile == null || sourceDeclarationMap == null ) return ;
        
        try (Stream<String> lines = Files.lines(Paths.get(connecFile))) {

	       lines.forEach ( line  -> {
                   if(line.replaceAll(" +", " ").trim().startsWith("obda-") && line.contains(":")) {
                     String[] splitedLines = line.replaceAll(" +", "").trim().split(":", 2) ;
                     if( splitedLines.length >= 2 ) 
                     sourceDeclarationMap.put(  splitedLines[0].trim().replace("obda-", "") , 
                                                splitedLines[1].trim()) ;
                   }
               }) ;

	} catch (IOException e) {
		e.printStackTrace();
	}
    }

}
