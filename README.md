# Cross-Account Cloudformation Stack Resource Provider

This AWS CloudFormation resource provider implements the creation/update/delete of a AWS CloudFormation stack cross-account

## ProServe::CloudFormation::Stack

See example usage of the resource below. Detailed documentation can be found in the [/docs](docs) folder.

## Usage

Simple Example:

```yaml
TestStack:
  Type: ProServe::CloudFormation::Stack
  Properties:
    AccountId: 999999999999
    AssumeRoleName: OrganizationAccountAccessRole
    StackName: myTestStack
    Template: |
        Description: myTestStack
        Parameters:
          TestParam:
            Type: String
        Resources:
          DummyParameter:
            Type: AWS::SSM::Parameter
            Properties:
              Name: /test-stack/abc123
              Value: !Ref TestParam
              Type: String
    Parameters:
      - Key: TestParam
        Value: abc123
    Tags:
      - Key: mycorp:CostCenter
        Value: ABC123
```

## Quickstart

You can use the following link to deploy the CloudFormation resource provider directly into your AWS account. Ensure you are logged into the AWS Console before following it.
After following the link, ensure you picked the desired **Region** on the top right within the AWS Console.

[Quickstart CloudFormation Link](https://console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/new?templateURL=https:%2F%2Fs3.amazonaws.com%2Faws-enterprise-jumpstart%2Faws-cloudformation-stack-resource%2Fcfn-provider-registration.yaml)

## Hints

* While code samples in this repository has been tested and believe it works well, as always, be sure to test it in your environment before using it in production!

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the MIT-0 License. See the LICENSE file.

