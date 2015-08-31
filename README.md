# box-connector

Example project connector for mPath.

For every request, mPath will pass an Authorization header. This header can be used for various phases of the OAuth flow.

## Usage
````bash
>> gradle shadowJar
>> java -jar build/libs/box-connector-all.jar
````

To test, first grab a Developer token from Box.
And then run the following command:
````bash
>> curl -i -H 'Authorization: ZBkiyyKgtv0XrlV31teXDzwGj0OSoucF' \
>>         -H 'Content-Type: application/json' \
>>         localhost:4567/your-folder-id
````
