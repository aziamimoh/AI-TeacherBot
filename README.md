# AI-Teacherbot

AI-Teacher is a Java program that aims to help students study more 
efficiently by quizzing them on concepts from material they provide 
to the program.

## Installation and Usage

* Make sure to add as shell variable for openai token to your environment
```
export OPENAI_TOKEN=XXXX
```
* Load project into Intellij
* Click on 'Run'

Press ENTER
```java
Enter input text file (default ./input.txt):
```    
Enter your answer after `.. as mentioned in the reference:`   
In the example below, the user's response is `'Design involves planning'`

The rest of the text is from OpenAI
```
Please write about what design means, as mentioned in the reference: Design involves planning
The user's note is a simplified statement that design involves planning. 

The reference note, on the other hand, explains how scientists use models and experimental results to construct explanations or design solutions. It provides an example of designing the body shape of a car to be more aerodynamic.
```


## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[Apache](https://github.com/apache/opennlp/blob/main/LICENSE)
