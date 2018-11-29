# SPA tool page

## Overview

This tool introduces the concept of *Systems of Procedural Automata* (SPAs), an extension to DFAs to systems of DFAs that can mutually call each other.
SPAs allow for an intuitive representation of procedural software systems, i.e. systems modeled after context-free languages by means of the copy-rule semantic, where terminal symbols represent internal actions and non-terminal symbols represent method invocations.

The tool provides an active learning algorithm for systems that are instrumented such that the start and the end of the procedures are observable.
This instrumentation is natural to imperative and procedural software systems and is oftentimes already part of the system itself (see the pedigree XML example of the tool).

#### Contribution to AutomataLib and LearnLib

Currently, the only model both [AutomataLib][1] and [LearnLib][0] support, that is able to formalize the hierarchical call structure of software systems, is that of *visibly pushdown automata* (VPAs).
Our proposed formalism for SPAs extends both libraries by being able to model and learn arbitrary context-free systems (the system of palindromes is a prominent example of a context-free system that is **not** a visibly pushdown system).

Our proposed instrumentation is very powerful.
Indeed, it also enables VPA learning to cover all context-free languages.
In order to illustrate the difference between SPA learning and VPA learning after instrumentation, please consider the following grammar for palindromes (which we also briefly discuss in the paper):

```
S -> a | a S a | b | b S b | T | ε
T -> c | c T c | S
```

<p align="center">
  <a href="docs/palin_spa.png"><img src="docs/palin_spa.png" alt="SPA model" width="45%" align="middle"/></a>
  <a href="docs/palin_vpa.png"><img src="docs/palin_vpa.png" alt="VPA model" width="45%" align="middle"/></a>
<p>

(Click on the images for a full-sized view)

Both models shown above (for SPA on the left and for VPA on the right) were inferred via black-box testing as described in the paper.
Although both models capture in essence the same information, there is a big difference in their comprehensibility:
Whereas the former very intuitively reflects the underlying grammar, the latter is quite  hard to understand - and, in fact, also more expensive to infer (see the quantitative results below).
As the required instrumentation is very natural, we are convinced that SPA learning has quite some potential for practical application.


## Installation Guide

* #### Java 8 & Maven
  The tool requires a working [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) & [Maven](https://maven.apache.org/) installation.
  It is suggested, to use one of the major Java IDEs, which should already have out-of-the-box support for Maven projects.

* #### GraphViz
  For visualizing the automata, [GraphViz](http://www.graphviz.org/) is required.

## Using the tool / Examples

The tool consists of two modules -- **learner** and **examples**.

* ### learner

  The **learner** module contains the core components of the learning algorithm.

  - The `SPALearner` class resembles the main class of the learning algorithm and integrates into the `LearningAlgorithm` framework of the LearnLib.
  - The `InstrumentedStackAcceptor` class resembles a stack-based implementation that accepts the language of an instrumented SPA.
  - The `config` package contains several predefined adapters that can be used to configure, which learning algorithms the `SPALearner` should use for learning the individual sub-procedures of the system under learning.
  - The `view` package contains two different views for displaying the structure of an SPA.
    The `InternalView` renders the sub-procedures as-is in the form of DFAs.
    The `ReachabilityView` only renders states and transitions on paths to accepting states, similar to the figures of the paper.

* ### examples

  The **examples** module holds several classes that display the usage and application of the tool.
  It contains several benchmarks for which we want to briefly sketch the quantitative and qualitative results in the following.

  Note, for generating counterexamples in our black-box testing scenario, we (in order) generated 10000 random, well-matched, rooted words of maximum length 100, for CFG based approaches (Palindromes, Exponential) generated 10000 words of the (instrumented) context-free language and finally sampled from a manually constructed set of characteristic words to ensure termination against the correct model.
  The results for the quantitative evaluations are averaged over 10 learning runs.

  ### Palindromes

  A qualitative comparison of the palindrome examples has already been presented earlier.
  The following shows the quantitative comparison of the different learning scenarios.

  |                          | #CEs | #Queries | #Symbols | Hyp. size |
  |:------------------------ | ----:| --------:| --------:| ---------:|
  | SPA [L* (classic)]       |  7.4 |    435.7 |   3835.0 |      12.0 |
  | SPA [L* (Riv. & Schap.)] |  8.4 |    312.9 |   2354.2 |      12.0 |
  | SPA [L* (Kearns & Vaz.)] |  8.9 |    215.6 |   1484.2 |      12.0 |
  | SPA [DiscriminationTree] |  8.8 |    219.1 |   1483.2 |      12.0 |
  | SPA [TTT]                |  9.1 |    184.8 |   1246.2 |      12.0 |
  | VPA [DiscriminationTree] |  8.1 |   1643.3 |  15527.0 |      11.0 |
  | VPA [TTT]                |  8.3 |   1615.3 |  15234.3 |      11.0 |

  As can be seen, our SPA approach outperforms the VPA approach by a significant margin, even for this small of an example.

  ### XML

  XML documents are a highly practical example, where our orchestration naturally integrates into the given structure:
  When interpreting XML tags as procedures, the start and end of a procedure is given by its opening and closing tag.

  For our example, consider the following DTD (document type definition):
  ```xml
  <!ELEMENT person ((mother | father)? |
                   (father, mother) |
                   (mother, father))>
  <!ELEMENT mother (person)>
  <!ELEMENT father (person)>

  <!ATTLIST person name CDATA #REQUIRED>
  <!ATTLIST person died CDATA #IMPLIED>
  ```

  Documents adhering to this DTD allow to store information about the pedigree of a person.
  Each person has to be identified via its `name` attribute and may optionally store information about his/her year of death (if the person died already). Furthermore, it is possible to store information about his/her ancestors via nested `<mother>` or `<father>` tags (optional and irrespective of order), which themselves hold persons again.

  By translating abstract queries (e.g. `PERSON name R`) to concrete XML documents (i.e `<person name="..."></person>`), we can learn the structure of DTDs solely by asking a black-box DTD validator, if a provided XML document conforms to an (unknown, to be inferred) DTD or not.
  In this example, we have modeled {`PERSON`, `MOTHER`, `FATHER`} as call symbols representing the tags and {`name`, `died`} as internal symbols representing attributes.
  The following shows the inferred models:

  <p align="center">
    <a href="docs/pedigree_spa.png"><img src="docs/pedigree_spa.png" alt="SPA model" width="45%" align="middle"/></a>
    <a href="docs/pedigree_vpa.png"><img src="docs/pedigree_vpa.png" alt="VPA model" width="45%" align="middle"/></a>
  <p>
  (Click on the images for a full-sized view)

  As one can see, from the SPA model it is directly possible to infer the nesting structure of the different procedures (tags).
  Furthermore, given that attributes were modeled as internal symbols, one can deduce that the `name` attribute is mandatory, whereas the `died` attribute is completely optional.
  One can reconstruct the structure of the (previously unknown) DTD, given the model of the SPA.
  In contrast to that is the VPA model, which with its explicit stack management, makes it hard to understand the key structure of the document.

  Regarding performance, the following table again shows that our SPA approach additionally allows to reduce the query complexity.
  In this case an improvement around a factor of 3-4 was achieved.
  We can even see, that for the DT-variant of the VPA learner, there are instances where the counterexamples of the equivalence oracle (which checked a set of characteristic traces) failed to uncover all hypothesis states, as the average hypothesis size is below 9.

  |                          | #CEs | #Queries | #Symbols | Hyp. size |
  |:------------------------ | ----:| --------:| --------:| ---------:|
  | SPA [L* (classic)]       |  8.3 |    501.1 |   6592.0 |      14.0 |
  | SPA [L* (Riv. & Schap.)] |  8.2 |    344.6 |   4101.8 |      14.0 |
  | SPA [L* (Kearns & Vaz.)] |  9.2 |    223.6 |   2748.1 |      14.0 |
  | SPA [DiscriminationTree] |  9.3 |    234.1 |   2864.5 |      14.0 |
  | SPA [TTT]                |  9.5 |    204.4 |   2631.3 |      14.0 |
  | VPA [DiscriminationTree] |  6.8 |    901.3 |   9965.6 |       8.9 |
  | VPA [TTT]                |  6.7 |    853.2 |   9245.0 |       9.0 |


  ### Regular Exponential Systems

  Even in cases, where the structure of software systems is not necessarily context-free but exhibits a highly repetitive structure, the correct abstraction level can be beneficial for its understanding and learning performance.

  Consider, for example, the following procedural system

  ```
  A -> B B B B
  B -> C C C C
  C -> D D D D
  D -> a b c
  ```

  This system exhibits a regular structure and thus can also be learned in its non-instrumented form with arbitrary regular learning algorithms.
  The following shows the inferred models and performance measurements for the SPA, VPA and regular (non-instrumented) configuration.

  <p align="center">
    <a href="docs/exponential_spa.png"><img src="docs/exponential_spa.png" alt="SPA model" width="30%" align="middle"/></a>
    <a href="docs/exponential_vpa.png"><img src="docs/exponential_vpa.png" alt="VPA model" width="30%" align="middle"/></a>
    <a href="docs/exponential_reg.png"><img src="docs/exponential_reg.png" alt="Regular model" width="30%" align="middle"/></a>
  <p>
  (Click on the images for a full-sized view. There are 3 images)

  |                          | #CEs |  #Queries |    #Symbols | Hyp. size |
  |:------------------------ | ----:| ---------:| -----------:| ---------:|
  | SPA [L* (classic)]       |  5.0 |    1320.0 |    565376.0 |      23.0 |
  | SPA [L* (Riv. & Schap.)] |  5.0 |     868.0 |    355863.0 |      23.0 |
  | SPA [L* (Kearns & Vaz.)] |  5.0 |     856.0 |    347419.0 |      23.0 |
  | SPA [DiscriminationTree] |  5.0 |     855.0 |    348077.0 |      23.0 |
  | SPA [TTT]                |  5.0 |     906.0 |    387557.0 |      23.0 |
  | VPA [DiscriminationTree] |  2.0 |   22936.0 |  10124646.0 |      18.0 |
  | VPA [TTT]                |  2.0 |   22971.0 |  10137135.0 |      18.0 |
  | REG [L* (classic)]       |  2.0 |  223687.0 |  43264617.0 |     194.0 |
  | REG [Rivest/Schapire]    |  2.0 |  114007.0 |  21798161.0 |     194.0 |
  | REG [Kearns/Vazirani]    |  2.0 | 1292135.0 | 132508009.0 |     194.0 |
  | REG [DiscriminationTree] |  2.0 |   95673.0 |  19383730.0 |     194.0 |
  | REG [TTT]                |  2.0 |   95661.0 |  19470481.0 |     194.0 |

  Again, our SPA approach directly allows to infer the underlying structure of the system, while performing best among its competitors -- this time even an order of magnitude better.
  As the regular non-instrumented setup shows, even for this simple example a regular abstraction is in no way feasible for learning inherently procedural systems.

## Acknowledgments

For building membership oracles and equivalence oracles based on context-free grammars (i.e. answering the membership question and iterating over words of the CFL in search for a counterexample) we use the CFG toolkit of [Zhiwu Xu](http://lcs.ios.ac.cn/~zhiwu/).
The toolkit was originally downloaded from the [toolkit's homepage](http://lcs.ios.ac.cn/~zhiwu/toolkit.php).

[0]: https://github.com/LearnLib/learnlib
[1]: https://github.com/LearnLib/automatalib
